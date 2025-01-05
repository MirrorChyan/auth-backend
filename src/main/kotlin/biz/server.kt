@file:JvmName("Server")

package biz

import io.vertx.core.Vertx
import org.slf4j.LoggerFactory
import router.router

private val log = LoggerFactory.getLogger("ServerKt")!!
const val PORT = 9768

fun listen() {
    Vertx.vertx().apply {
        createHttpServer().let {
            router(this, it)
            it.listen(PORT)
        }
    }
    log.info("Mirrorc-CDK-Backend is running on port $PORT")
}