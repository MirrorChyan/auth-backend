package biz

import cache.CT_CACHE
import com.alibaba.fastjson2.JSON
import datasource.DB
import model.CreateCdkTypeParams
import model.CreateTokenParams
import model.Resp
import model.entity.ApplicationToken
import model.entity.CDKType
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
fun validateToken(token: String?, resourceId: String): Resp {
    val tk = token.throwIfNullOrEmpty("The Token cannot be empty", 400)
    val qr = DB.from(ApplicationToken)
        .select(ApplicationToken.status, ApplicationToken.id, ApplicationToken.resourceList)
        .where {
            ApplicationToken.applicationToken eq tk
        }
        .limit(1)
        .iterator()
    qr.hasNext().throwIfNot("Invalid Token")

    qr.next().apply {
        (get(ApplicationToken.status) != 1).throwIf("The Token status is incorrect")
        val list = get(ApplicationToken.resourceList)
        if (list != null) {
            JSON.parseArray(list, String::class.java).contains(resourceId)
                .throwIfNot("The resource cannot be uploaded using this token")
        }
    }

    return Resp.success()
}

fun createCdkType(params: CreateCdkTypeParams): Resp {
    with(params) {
        type.throwIfNullOrEmpty("typeId cannot be empty")
        resourceIdList.throwIfNullOrEmpty("resourceIdList can't be empty")
    }
    val exist = DB.from(CDKType).select(CDKType.typeId)
        .where {
            CDKType.typeId eq params.type!!
        }.limit(1).iterator().hasNext()
    val tip = if (exist) {
        DB.update(CDKType) {
            set(CDKType.resourcesGroup, JSON.toJSONString(params.resourceIdList))
        }
        "update success"
    } else {
        DB.insert(CDKType) {
            set(CDKType.typeId, params.type)
            set(CDKType.resourcesGroup, JSON.toJSONString(params.resourceIdList))
        }
        "add success"
    }

    CT_CACHE.invalidate(params.type)

    return Resp(0, tip)
}

fun createApplicationToken(params: CreateTokenParams): Resp {
    val list = params.resourceIdList
    val uuid = UUID.randomUUID().toString()

    DB.insert(ApplicationToken) {
        if (list != null) {
            set(ApplicationToken.resourceList, JSON.toJSONString(list))
        }
        set(ApplicationToken.applicationToken, uuid)
    }
    return Resp.success(uuid)
}