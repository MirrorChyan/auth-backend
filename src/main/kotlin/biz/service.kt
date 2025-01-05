package biz

import datasource.DB
import exception.ServiceException
import model.PlanParams
import model.Resp
import model.ValidateParams
import model.entity.CDK
import model.entity.OperationLog
import org.ktorm.dsl.*
import utils.throwIf
import utils.throwIfNot
import java.nio.ByteBuffer
import java.time.LocalDateTime
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs

const val ST: Int = 0x1bf52

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


fun acquire(params: PlanParams): Resp {
    with(params) {
        (params.expireTime == null || params.expireTime!!.isBefore(LocalDateTime.now())).throwIf("过期时间设置有误")
    }

    val eTime = params.expireTime!!

    val key = next()

    DB.insert(CDK) {
        set(CDK.key, key)
        set(CDK.expireTime, eTime)
    }

    return Resp.success(key)

}


fun validate(params: ValidateParams): Resp {
    with(params) {
        specificationId.isNullOrBlank().throwIf("specificationId不能为空")
        cdk.isNullOrBlank().throwIf("CDK不能为空")
    }
    val cdk = params.cdk!!
    val specId = params.specificationId!!
    val qr = DB.from(CDK)
        .select(CDK.expireTime, CDK.specificationId)
        .where {
            CDK.key eq cdk
        }
        .iterator()
    if (!qr.hasNext()) {
        throw ServiceException("invalid cdk")
    }
    val next = qr.next();
    val expireTime = next[CDK.expireTime]!!

    expireTime.isBefore(LocalDateTime.now()).throwIf("CDK已过期")

    val eId = next[CDK.specificationId]
    if (eId == null) {
        (DB.update(CDK) {
            where {
                CDK.key eq cdk
            }
            set(CDK.specificationId, specId)
        } > 0).throwIfNot("CDK绑定更新失败")
    } else {
        (eId != specId).throwIf("CDK已被使用")
    }

    // log
    DB.insert(OperationLog) {
        set(OperationLog.cdk, cdk)
        set(OperationLog.source, params.source)
        set(OperationLog.specificationId, specId)
        set(OperationLog.type, "validate")
    }


    // TODO 重定向下载地址

    return Resp.success(1)
}