package model

import java.time.LocalDateTime


/**
 * @author Alioth
 * @date 2025/01/03
 * @constructor 创建[PlanParams]
 */
class PlanParams {
    /**
     * 类型
     *  */
    var type: String = ""

    /**
     * 过期时间
     *  */
    var expireTime: LocalDateTime? = null

    var typeId: String? = null
}

class RenewParams {
    /**
     * CDK
     * */
    var cdk: String? = null

    /**
     * 续期后的过期时间
     *  */
    var expireTime: LocalDateTime? = null
}

/**
 * @author Alioth
 * @date 2025/01/03
 * @constructor 创建[ValidateParams]
 */
class ValidateParams {

    /**
     * CDK
     * */
    var cdk: String? = null

    /**
     * 资源类型
     *  */
    var resource: String? = null

    /**
     * user-agent
     *  */
    var ua: String? = null

    /**
     * ip
     */
    var ip: String? = ""

    /**
     * version
     */
    var version: String? = null

    /**
     *
     * filesize
     */
    var filesize: Long? = null
}


class CdkTypeParams {
    var type: String? = null

    /**
     * 可适用的资源ID
     */
    var resourceIdList: List<String>? = null
}

class CreateTokenParams {

    /**
     * 可适用的资源ID
     */
    var resourceIdList: List<String>? = null
}
