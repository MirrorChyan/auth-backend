package biz


import cache.BT
import cache.C
import cache.CT_CACHE
import cache.DOWNLOAD_TRIGGER
import com.alibaba.fastjson2.JSON
import config.Props
import config.RDS
import datasource.DB
import model.*
import model.entity.CDK
import model.entity.CDKType
import org.ktorm.dsl.*
import org.slf4j.LoggerFactory
import stat.StatHelper
import utils.throwIf
import utils.throwIfNot
import utils.throwIfNullOrEmpty
import java.nio.ByteBuffer
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs

const val ST: Int = 0xBADF00D

private val NEXT_INC = AtomicInteger(ThreadLocalRandom.current().nextInt())

private val CIR = intArrayOf(1, 4, 5, 9, 8, 1, 5)


private val log = LoggerFactory.getLogger("CDKEYKt")!!

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

    val typeId = params.typeId ?: return Resp.fail("please set the typeId")
    val key = next()

    DB.insert(CDK) {
        set(CDK.key, key)
        set(CDK.expireTime, eTime)
        set(CDK.typeId, typeId)
    }

    C.invalidate(key)

    return Resp.success(key)

}

private const val VALIDATE = "validate"
private const val tips = "Please confirm that you have entered the correct cdkey"
private const val limitTips = "Your cdkey has reached the most downloads today"

private val EMPTY = ValidTuple(-1, LocalDateTime.now(), "", -1)


private fun getCDKInfo(cdk: String) = run {
    val qr = DB.from(CDK)
        .select(CDK.expireTime, CDK.status, CDK.typeId, CDK.limit)
        .where {
            CDK.key eq cdk
        }
        .iterator()
    if (qr.hasNext()) {
        qr.next().run {
            ValidTuple(
                this[CDK.status]!!,
                this[CDK.expireTime]!!,
                this[CDK.typeId]!!,
                this[CDK.limit]!!
            )
        }
    } else {
        EMPTY
    }
}

fun validateCDK(params: ValidateParams): Resp {

    val cdk = params.cdk
    if (cdk.isNullOrBlank() || cdk.length > 30 || cdk.length < 10) {
        return Resp.fail(tips, KEY_INVALID)
    }

    val record = C.get(cdk, ::getCDKInfo)

    // cache empty
    if (record.status == -1) {
        return Resp.fail(tips, KEY_INVALID)
    }

    if (record.status == 3) {
        return Resp.fail(tips, KEY_BLOCKED)
    }

    val expireTime = record.expireTime
    val status = record.status

    val timeout = expireTime.isBefore(LocalDateTime.now())
    if (timeout) {
        return Resp.fail("The cdk has expired", KEY_EXPIRED)
    }

    if (!doLimit(cdk, record.limit)) {
        log.warn("cdk limit download {}", cdk)
        return Resp.fail(limitTips, RESOURCE_QUOTA_EXHAUSTED)
    }

    val checked = checkCdkType(record.typeId, params.resource)

    if (!checked) {
        return Resp.fail("Current cdk cannot download this resource, please check your cdk type", KEY_MISMATCHED)
    }

    StatHelper.offer(cdk)

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

    val resource = params.resource ?: ""
    val ua = params.ua ?: "$resource-NoUA"
    doSendBillingCheckIn(cdk, resource, ua)

    // log
    BT.enqueue(
        LogRecord(
            cdk = cdk,
            resource = resource,
            type = VALIDATE,
            ua = ua,
            ip = params.ip,
            time = LocalDateTime.now()
        )
    )

    return Resp.success()
}


private fun checkCdkType(typeId: String?, resource: String?): Boolean {
    if (typeId == null || resource == null) {
        return false
    }

    val set = CT_CACHE.get(typeId) {
        val itr = DB.from(CDKType)
            .select(CDKType.resourcesGroup)
            .where {
                CDKType.typeId eq typeId
            }
            .limit(1)
            .iterator()

        when {
            itr.hasNext() -> itr.next()[CDKType.resourcesGroup]
                ?.takeIf { it.isNotEmpty() }
                ?.let { JSON.parseArray(it, String::class.java).toHashSet() }
                ?: emptySet()

            else -> emptySet()
        }
    }

    return set.contains(resource)
}


fun validateDownload(info: ValidateParams): Resp {
    val cdk = info.cdk ?: ""
    if (cdk.isBlank() || cdk.length > 30 || cdk.length < 10) {
        return Resp.fail(limitTips, RESOURCE_QUOTA_EXHAUSTED)
    }

    val record = C.get(cdk, ::getCDKInfo)

    // cache empty
    if (record.status == -1 || record.status == 3) {
        return Resp.fail(tips, KEY_INVALID)
    }

    val k = KeyGenerator(cdk)
    val cnt = RDS.get().incr(k) ?: 1L
    if (cnt == 1L) {
        RDS.get().expire(k, Duration.ofDays(1))
    }
    if (cnt - 1 < record.limit) {
        DOWNLOAD_TRIGGER.enqueue(
            DownloadRecord(
                cdk = cdk,
                resource = info.resource ?: "",
                ua = info.ua ?: "",
                ip = info.ip ?: "",
                version = info.version ?: "",
                filesize = info.filesize ?: 0L,
                time = LocalDateTime.now()
            )
        )
        return Resp.success()
    }

    return Resp.fail(limitTips, RESOURCE_QUOTA_EXHAUSTED)
}

private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
private val KeyGenerator: (String) -> String = {
    val date = LocalDate.now().format(DATE_FORMATTER)
    "limit:${date}:${it}"
}

private fun doLimit(cdk: String, limit: Int): Boolean {
    if (!Props.Extra.limitEnabled) {
        return true
    }
    val cnt = RDS.get().get(KeyGenerator(cdk))?.toIntOrNull() ?: 0

    return cnt < limit
}

fun recoverLimit(cdk: String): Resp {
    val r = RDS.get().del(KeyGenerator(cdk))
    log.info("recover cdk limit {}", cdk)
    return Resp.success(r)
}