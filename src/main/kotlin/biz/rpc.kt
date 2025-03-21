package biz

import config.Props
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.concurrent.Executors

private val log = LoggerFactory.getLogger("RpcKt")!!

val C: OkHttpClient = OkHttpClient.Builder()
    .dispatcher(Dispatcher(Executors.newVirtualThreadPerTaskExecutor()))
    .build()

private val JSON_MT = "application/json; charset=utf-8".toMediaType()


private val H = object : Callback {
    override fun onFailure(call: Call, e: IOException) {
        log.error("BillingCheckIn response", e)
    }

    override fun onResponse(call: Call, response: Response) {
        response.use { r ->
            if (r.code != 200) {
                log.error("BillingCheckIn response {}", r.body?.string())
            }
        }
    }
}

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
            C.newCall(it).enqueue(H)
        }

}
