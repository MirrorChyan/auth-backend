package biz

import config.Props
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("RpcKt")!!

val C: OkHttpClient = OkHttpClient.Builder()
    .build()

private val JSON_MT = "application/json; charset=utf-8".toMediaType()

fun doSendBillingCheckIn(cdk: String, resource: String, ua: String) {
    val body = """
        {
            "cdk": "$cdk",
            "application": "$resource",
            "user_agent": "$ua",
            "module": ""
        }
    """.trimIndent().toRequestBody(contentType = JSON_MT)
    Request.Builder()
        .url(Props.Extra.billingCheckInUrl)
        .post(body)
        .build().let {
            C.newCall(it).execute()
        }.use {
            if (it.code != 200) {
                log.error("BillingCheckIn response {}", it.body?.string())
            }
        }

}
