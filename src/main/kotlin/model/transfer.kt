package model


data class LogRecord(
    val cdk: String,
    val source: String?,
    val spId: String,
    val type: String,
)