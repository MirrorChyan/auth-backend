package model.entity

import org.ktorm.schema.*

object CDK : Table<Nothing>("mirrorc_cdk") {
    val key = varchar("key").primaryKey()
    val specificationId = varchar("specification_id")
    val type = varchar("type")

    // 0 未使用 1 已使用 3 封禁 -1 忽略
    val status = int("status")
    val limit = int("limit")
    val typeId = varchar("type_id")
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

object DownloadLog : Table<Nothing>("mirrorc_download_log") {
    val id = int("id").primaryKey()
    val cdk = varchar("cdk")
    val ip = varchar("ip")
    val ua = varchar("ua")
    val resource = varchar("resource")
    val filesize = long("filesize")
    val version = varchar("version")
    val createdAt = datetime("created_at")
}


object CDKType : Table<Nothing>("mirrorc_cdk_type") {
    val typeId = varchar("type_id").primaryKey()
    val resourcesGroup = varchar("resources_group")
    val createdAt = datetime("created_at")
}

object ApplicationToken : Table<Nothing>("mirrorc_application_token") {
    val id = int("id").primaryKey()
    val resourceList = varchar("resource_list")
    val applicationToken = varchar("application_token")

    /*
     * 1 正常
     */
    val status = int("status")
    val createdAt = datetime("created_at")
}