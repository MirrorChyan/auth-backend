package model.entity

import org.ktorm.schema.Table
import org.ktorm.schema.datetime
import org.ktorm.schema.int
import org.ktorm.schema.varchar

object CDK : Table<Nothing>("mirrorc_cdk") {
    val key = varchar("key").primaryKey()
    val specificationId = varchar("specification_id")
    val type = varchar("type")
    // 0 未使用 1 已使用
    val status = int("status")
    val expireTime = datetime("expire_time")
    val createdAt = datetime("created_at")
}

object OperationLog : Table<Nothing>("mirrorc_operation_log") {
    val id = int("id").primaryKey()
    val cdk = varchar("cdk")
    val specificationId = varchar("specification_id")
    val ip = varchar("ip")
    val ua = varchar("ua")
    val resource = varchar("resource")
    val type = varchar("type")
    val createdAt = datetime("created_at")
}

object Application : Table<Nothing>("mirrorc_application") {
    val id = int("id").primaryKey()
    val applicationName = varchar("application_name")
    val createdAt = datetime("created_at")
}

object ApplicationToken : Table<Nothing>("mirrorc_application_token") {
    val id = int("id").primaryKey()
    val applicationId = int("application_id")
    val applicationToken = varchar("application_token")
    val status = int("status")
    val createdAt = datetime("created_at")
}