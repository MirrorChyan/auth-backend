package router

import biz.acquire
import biz.validate
import com.alibaba.fastjson2.JSON
import exception.ServiceException
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServer
import io.vertx.ext.web.Router
import io.vertx.ext.web.Router.router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import model.PlanParams
import model.Resp
import model.ValidateParams
import org.slf4j.LoggerFactory
import utils.execute

private val log = LoggerFactory.getLogger("RouterKt")!!

const val UEE = """{"code":-1,"msg":"真不幸,意料之外的错误"}"""

/**
 * @param [vertx]
 * @param [server]
 */
fun router(vertx: Vertx, server: HttpServer) {
    val router = router(vertx).apply {
        route().handler(BodyHandler.create())
        route().last().failureHandler(::errHandler)
        dispatch(this)
    }
    server.requestHandler(router)
}

private fun errHandler(ctx: RoutingContext) {
    val ex = ctx.failure()
    val resp = ctx.response()
    resp.putHeader("Content-Type", "application/json")
    resp.setStatusCode(500)
    if (ex is ServiceException) {
        ctx.end(Resp.fail(ex.message).toJson())
        log.error("ServiceException {}", ex.message)
        return
    }
    log.error("unexpected error ", ex)
    ctx.end(UEE)
}

private fun requireJsonParams(ctx: RoutingContext): String? {
    val body = ctx.body().asString()
    if (body == null || ctx.request().headers().get("content-type") != "application/json") {
        ctx.response().setStatusCode(400)
        ctx.end(Resp.fail("参数不合法").toJson())
    }
    return body

}

/**
 * @param [router]
 */
private fun dispatch(router: Router) {
    router.post("/acquire").handler { ctx ->
        requireJsonParams(ctx)?.let {
            val p = JSON.parseObject(it, PlanParams::class.java)
            ctx.response().putHeader("Content-Type", "application/json")
            Promise.promise<String>().execute(ctx) {
                acquire(p).toJson()
            }
        }
    }
    router.post("/validate").handler { ctx ->
        requireJsonParams(ctx)?.let {
            val p = JSON.parseObject(it, ValidateParams::class.java)
            ctx.response().putHeader("Content-Type", "application/json")
            Promise.promise<String>().execute(ctx) {
                validate(p).toJson()
            }
        }
    }
}