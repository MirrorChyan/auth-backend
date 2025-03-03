package router

import biz.*
import com.alibaba.fastjson2.JSON
import exception.ServiceException
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServer
import io.vertx.ext.web.Router
import io.vertx.ext.web.Router.router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import metrics.registry
import model.*
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
    if (ex is ServiceException) {
        ctx.response().setStatusCode(ex.code)
        ctx.end(Resp.fail(ex.message).toJson())
        log.error("ServiceException {}", ex.message)
        return
    }
    resp.setStatusCode(500)
    log.error("unexpected error ", ex)
    ctx.end(UEE)
}

private fun requireJsonParams(ctx: RoutingContext): String? {
    val body = ctx.body().asString()
    if (body == null || ctx.request().headers().get("content-type") != "application/json") {
        ctx.response().setStatusCode(400)
        ctx.end(Resp.fail("parameter invalid").toJson())
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
                acquireCDK(p).toJson()
            }
        }
    }

    router.post("/renew").handler { ctx ->
        requireJsonParams(ctx)?.let {
            val p = JSON.parseObject(it, RenewParams::class.java)
            ctx.response().putHeader("Content-Type", "application/json")
            Promise.promise<String>().execute(ctx) {
                renewCDK(p).toJson()
            }
        }

    }

    router.post("/validate").handler { ctx ->
        requireJsonParams(ctx)?.let {
            val p = JSON.parseObject(it, ValidateParams::class.java)
            ctx.response().putHeader("Content-Type", "application/json")
            Promise.promise<String>().execute(ctx) {
                validateCDK(p).toJson()
            }
        }
    }


    router.post("/create/cdk/type").handler { ctx ->
        requireJsonParams(ctx)?.let {
            val p = JSON.parseObject(it, CreateCdkTypeParams::class.java)
            ctx.response().putHeader("Content-Type", "application/json")
            Promise.promise<String>().execute(ctx) {
                createCdkType(p).toJson()
            }
        }
    }

    router.post("/develop/validate").handler { ctx ->
        val token: String? = ctx.request().getParam("token")
        val resourceId = ctx.request().getParam("rid") ?: ""
        ctx.response().putHeader("Content-Type", "application/json")
        Promise.promise<String>().execute(ctx) {
            validateToken(token, resourceId).toJson()
        }
    }

    router.post("/upload/token").handler { ctx ->
        requireJsonParams(ctx)?.let {
            ctx.response().putHeader("Content-Type", "application/json")
            Promise.promise<String>().execute(ctx) {
                CreateTokenParams().let { v ->
                    v.resourceIdList = JSON.parseArray(it, String::class.java)
                    createApplicationToken(v)
                }.toJson()
            }
        }
    }

    router.get("/metrics").handler { ctx ->
        val scrape = registry.scrape()
        ctx.response().apply {
            putHeader("Content-Type", "text/plain; version=0.0.4; charset=utf-8; escaping=values")
            putHeader("Content-Length", scrape.length.toString())
            write(scrape)
        }
        ctx.end()
    }

}