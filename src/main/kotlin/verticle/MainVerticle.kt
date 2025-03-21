package verticle

import biz.PORT
import io.vertx.core.AbstractVerticle
import org.slf4j.LoggerFactory
import router.router
import java.util.concurrent.atomic.AtomicInteger

private val log = LoggerFactory.getLogger("MainVerticle")!!

class MainVerticle : AbstractVerticle() {
    override fun start() {
        vertx.apply {
            createHttpServer().let {
                router(this, it)
                it.listen(PORT)
            }
        }
        log.info("Mirrorc-CDK-Backend Instance ${C.getAndIncrement()} is running on port $PORT")
    }
    companion object {
        val C = AtomicInteger(0)
    }
}