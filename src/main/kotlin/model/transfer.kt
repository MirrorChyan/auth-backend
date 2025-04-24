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

data class DownloadRecord(
    val cdk: String,
    val resource: String,
    val ua: String,
    val ip: String,
    val version: String,
    val filesize: Long,
    val time: LocalDateTime,
)

class ValidTuple(
    val status: Int,
    val expireTime: LocalDateTime,
    val typeId: String,
    val limit: Int
)