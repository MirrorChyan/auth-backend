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
    val tk = token.throwIfNullOrEmpty("The Token cannot be empty", 400)
    val qr = DB.from(ApplicationToken)
        .select(ApplicationToken.status, ApplicationToken.id)
        .where {
            ApplicationToken.applicationToken eq tk
        }
        .limit(1)
        .iterator()
    qr.hasNext().throwIfNot("Invalid Token")

    qr.next().apply {
        (get(ApplicationToken.status) != 1).throwIf("The Token status is incorrect")
    }

    return Resp.success()
}

/**
 * WIP
 * @param [applicationName]
 * @return [Resp]
 */
fun createApplication(applicationName: String?): Resp {
    applicationName.throwIfNullOrEmpty("The application name cannot be empty")

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
    applicationId.throwIfNullOrEmpty("The application Id cannot be empty")

    val uuid = UUID.randomUUID().toString()
    DB.insert(ApplicationToken) {
        set(ApplicationToken.applicationId, applicationId)
        set(ApplicationToken.applicationToken, uuid)
    }
    return Resp.success(uuid)
}