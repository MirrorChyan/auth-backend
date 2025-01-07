package biz

import datasource.DB
import model.Resp
import model.entity.Application
import model.entity.ApplicationToken
import org.ktorm.dsl.*
import utils.throwIf
import utils.throwIfNot
import utils.throwIfNullOrEmpty
import java.util.*

/**
 * Dev Token验证
 * @param [token]
 * @return [Resp]
 */
fun validateToken(token: String?): Resp {
    val tk = token.throwIfNullOrEmpty("Token不能为空")
    val qr = DB.from(ApplicationToken)
        .select(ApplicationToken.status, ApplicationToken.id)
        .where {
            ApplicationToken.applicationToken eq tk
        }
        .limit(1)
        .iterator()
    qr.hasNext().throwIfNot("Token无效")

    qr.next().apply {
        (get(ApplicationToken.status) != 1).throwIf("Token状态不正确")
    }

    return Resp.success()
}

/**
 * WIP
 * @param [applicationName]
 * @return [Resp]
 */
fun createApplication(applicationName: String?): Resp {
    applicationName.throwIfNullOrEmpty("应用名不能为空")

    DB.insertAndGenerateKey(Application) {
        set(Application.applicationName, applicationName)
    }
    return Resp.success()
}

/**
 * WIP
 * @param [applicationId]
 * @return [Resp]
 */
fun createApplicationToken(applicationId: Int?): Resp {
    applicationId.throwIfNullOrEmpty("应用Id不能为空")

    val uuid = UUID.randomUUID().toString()
    DB.insert(ApplicationToken) {
        set(ApplicationToken.applicationId, applicationId)
        set(ApplicationToken.applicationToken, uuid)
    }
    return Resp.success(uuid)
}