package model

import com.alibaba.fastjson2.JSON

data class Resp(
    val code: Int,
    val msg: String?,
    val data: Any? = null,
) {

    companion object {
        fun success(data: Any? = null) = Resp(0, "success", data)
        fun fail(msg: String?, code: Int = 1) = Resp(code, msg)
    }

    fun toJson(): String {
        return JSON.toJSONString(this)
    }
}