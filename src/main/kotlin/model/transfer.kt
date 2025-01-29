package model

import java.time.LocalDateTime


data class LogRecord(
    val cdk: String,
    val source: String?,
    val spId: String?,
    val type: String,
    val ua: String?,
    val time: LocalDateTime,
)

class ValidTuple(
    val status: Int,
    val expireTime: LocalDateTime,
    val spId: String?,
)