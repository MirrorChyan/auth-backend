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
}


/**
 * @author Alioth
 * @date 2025/01/03
 * @constructor 创建[ValidateParams]
 */
class ValidateParams {
    /**
     * 硬件ID
     *  */
    var specificationId: String? = null

    /**
     * CDK
     * */
    var cdk: String? = null

    /**
     * 来源
     *  */
    var source: String? = null
}
