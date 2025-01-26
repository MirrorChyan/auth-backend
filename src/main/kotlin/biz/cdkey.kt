package biz


import config.Props
import config.RDS
import datasource.DB
import exception.ServiceException
import model.PlanParams
import model.RenewParams
import model.Resp
import model.ValidateParams
import model.entity.CDK
import model.entity.OperationLog
import org.ktorm.dsl.*
import utils.throwIf
import utils.throwIfNot
import utils.throwIfNullOrEmpty
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.time.Duration
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

@OptIn(ExperimentalStdlibApi::class)
fun validateCDK(params: ValidateParams): Resp {
    with(params) {
        cdk.isNullOrBlank().throwIf("cdk cannot be empty")
    }
    val cdk = params.cdk!!
    val tmp = params.specificationId


    val qr = DB.from(CDK)
        .select(CDK.expireTime, CDK.specificationId, CDK.status)
        .where {
            CDK.key eq cdk
        }
        .iterator()
    if (!qr.hasNext()) {
        throw ServiceException("invalid cdk")
    }

    val next = qr.next()

    val expireTime = next[CDK.expireTime]!!
    val status = next[CDK.status]!!


    expireTime.isBefore(LocalDateTime.now()).throwIf("The cdk has expired")

    limit(cdk)


    val isFirstBinding = status == 0

    val oldSpecId = next[CDK.specificationId]
    val specId = when {
        tmp != null && oldSpecId == null -> {
            MessageDigest.getInstance("SHA-256").digest(tmp.toByteArray()).toHexString()
        }

        else -> null
    }
    if (isFirstBinding || specId != null) {
        val row = DB.update(CDK) {
            where {
                CDK.key eq cdk
            }
            if (isFirstBinding) {
                set(CDK.status, 1)
            }
            if (specId != null) {
                set(CDK.specificationId, specId)
            }
        }
        (row > 0).throwIfNot("cdk binding update failed")
    }


    // log
    DB.insert(OperationLog) {
        set(OperationLog.cdk, cdk)
        set(OperationLog.source, params.source)
        set(OperationLog.ua, params.ua)
        set(OperationLog.specificationId, specId ?: oldSpecId)
        set(OperationLog.type, "validate")
    }

    return Resp.success(isFirstBinding)
}

private fun limit(cdk: String) {
    if (!Boolean.equals(Props.Extra.limitEnabled)) {
        return
    }
    val date = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDateTime.now())
    val key = "limit:${date}:${cdk}"
    val cnt = RDS.get().get(key).get()?.toIntOrNull() ?: 0
    (cnt > Props.Extra.limitCount).throwIf("your account has been restricted for today")

    RDS.get().incr(key)

    if (cnt == 0) {
        RDS.get().expire(key, Duration.ofDays(1))
    }
}
