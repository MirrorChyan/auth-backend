package biz

import config.Props
import io.vertx.core.Vertx
import io.vertx.core.http.HttpClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("RpcKt")!!

val C: OkHttpClient = OkHttpClient.Builder()
    .build()

private val JSON_MT = "application/json; charset=utf-8".toMediaType()

private lateinit var client: HttpClient

fun setupClient(v: Vertx) {
    client = v.createHttpClient()
}

fun doSendBillingCheckIn(cdk: String, resource: String, ua: String) {
    val body = """
        {
            "cdk": "$cdk",
            "application": "$resource",
            "user_agent": "$ua",
            "module": "",
        }
    """.trimIndent().toRequestBody(contentType = JSON_MT)
    val resp = Request.Builder()
        .url(Props.Extra.billingCheckInUrl)
        .post(body)
        .build().let {
            C.newCall(it).execute()
        }
    if (resp.code != 200) {
        log.error("BillingCheckIn response {}", resp.body?.string())
    }
}
