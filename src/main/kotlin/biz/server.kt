package biz

import io.vertx.core.Vertx
import org.slf4j.LoggerFactory
import verticle.MainVerticle

private val log = LoggerFactory.getLogger("ServerKt")!!
const val PORT = 9768
fun listen() {
    Vertx.vertx().apply {
        deployVerticle(MainVerticle())
    }
}