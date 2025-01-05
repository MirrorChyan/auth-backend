package model

import java.time.LocalDateTime


/**
 * @author Alioth
 * @date 2025/01/03
 * @constructor 创建[PlanParams]
 */
class PlanParams {
    var type: String = ""
    var expireTime: LocalDateTime? = null
}


/**
 * @author Alioth
 * @date 2025/01/03
 * @constructor 创建[ValidateParams]
 */
class ValidateParams {
    var specificationId: String? = null
    var cdk: String? = null
    var source: String? = null
}
