package biz


import cache.BT
import cache.C
import config.Props
import config.RDS
import datasource.DB
import exception.ServiceException
import model.*
import model.entity.CDK
import org.ktorm.dsl.*
import utils.throwIf
import utils.throwIfNot
import utils.throwIfNullOrEmpty
import java.nio.ByteBuffer
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs

const val ST: Int = 0xBADF00D

private val NEXT_INC = AtomicInteger(ThreadLocalRandom.current().nextInt())

private val CIR = intArrayOf(1, 4, 5, 9, 8, 1, 5)

/**
 * 凑合用一下
 * @return [String]
 */
fun next(): String {
    val buf = ByteBuffer.wrap(ByteArray(12))
    buf.putInt(ST)
    buf.putInt((System.currentTimeMillis() / 100).toInt())
    buf.putInt(NEXT_INC.addAndGet(CIR[abs(NEXT_INC.get() % CIR.size)]))
    val sb = StringBuilder(24)
    val array = buf.array()
    var t: Int
    for (i in array) {
        t = i.toInt() and 0xff
        if (t < 16) {
            sb.append('0')
        }
        sb.append(Integer.toHexString(t))
    }
    return sb.toString()
}

fun renewCDK(params: RenewParams): Resp {
    with(params) {
        cdk.isNullOrBlank().throwIf("cdk cannot be empty")
        expireTime.throwIfNullOrEmpty("expireTime cannot be empty")
    }
    val cdk = params.cdk!!
    val expireTime = params.expireTime!!

    expireTime.isBefore(LocalDateTime.now()).throwIf("expireTime cannot be set to past")

    val row = DB.update(CDK) {
        where {
            CDK.key eq cdk
        }
        set(CDK.expireTime, expireTime)
    }
    (row > 0).throwIfNot("cdk renew failed")
    C.invalidate(cdk)

    return Resp.success()
}

fun acquireCDK(params: PlanParams): Resp {
    with(params) {
        (params.expireTime == null || params.expireTime!!.isBefore(LocalDateTime.now())).throwIf("The expiration time is incorrectly set")
    }

    val eTime = params.expireTime!!

    val key = next()

    DB.insert(CDK) {
        set(CDK.key, key)
        set(CDK.expireTime, eTime)
    }

    return Resp.success(key)

}

private const val VALIDATE = "validate"
private const val tips = "Please confirm that you have entered the correct cdkey"

fun validateCDK(params: ValidateParams): Resp {

    val cdk = params.cdk
    if (cdk.isNullOrBlank()) {
        throw ServiceException(tips)
    }

    val record = C.get(cdk) {


        val qr = DB.from(CDK)
            .select(CDK.expireTime, CDK.specificationId, CDK.status)
            .where {
                CDK.key eq cdk
            }
            .iterator()
        if (!qr.hasNext()) {
            throw ServiceException(tips)
        }
        qr.next().run {
            ValidTuple(
                this[CDK.status]!!,
                this[CDK.expireTime]!!,
            )
        }
    }

    val expireTime = record.expireTime
    val status = record.status
    expireTime.isBefore(LocalDateTime.now()).throwIf("The cdk has expired")

    doLimit(cdk)


    val isFirstBinding = status == 0


    if (isFirstBinding) {

        // ignore extreme races for now
        val row = DB.update(CDK) {
            where {
                CDK.key eq cdk
            }
            set(CDK.status, 1)
        }

        C.invalidate(cdk)
        (row > 0).throwIfNot("cdk binding update failed")
    }

    Thread.startVirtualThread {
        doSendBillingCheckIn(cdk, params.resource ?: "", params.ua ?: "")
    }

    // log
    BT.enqueue(
        LogRecord(
            cdk = cdk,
            resource = params.resource,
            type = VALIDATE,
            ua = params.ua,
            ip = params.ip,
            time = LocalDateTime.now()
        )
    )

    return Resp.success()
}

private fun doLimit(cdk: String) {
    if (!Props.Extra.limitEnabled) {
        return
    }
    val date = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDateTime.now())
    val key = "limit:${date}:${cdk}"
    val cnt = RDS.get().get(key).get()?.toIntOrNull() ?: 0
    (cnt > Props.Extra.limitCount).throwIf("Your cdkey has reached the most downloads today")

}
