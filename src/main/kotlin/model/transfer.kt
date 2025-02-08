package model

import java.time.LocalDateTime


data class LogRecord(
    val cdk: String,
    val resource: String?,
    val type: String,
    val ua: String?,
    val ip: String?,
    val time: LocalDateTime,
)

class ValidTuple(
    val status: Int,
    val expireTime: LocalDateTime,
)