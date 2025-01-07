package utils

import exception.ServiceException
import io.vertx.core.Promise
import io.vertx.ext.web.RoutingContext

fun Boolean.throwIf(msg: String) {
    if (this) {
        throw ServiceException(msg)
    }
}

fun <T> T?.throwIfNullOrEmpty(msg: String, code: Int = 200): T {
    if (this == null || (this is String && this.isEmpty())) {
        throw ServiceException(msg, code)
    }
    return this
}

fun Boolean.throwIfNot(msg: String) {
    (!this).throwIf(msg)
}


fun Promise<String>.execute(ctx: RoutingContext, block: () -> String) {
    val promise = Promise.promise<String>().apply {
        future().apply {
            onFailure(ctx::fail)
            onSuccess(ctx::end)
        }
    }
    Thread.startVirtualThread {
        try {
            val result = block()
            promise.complete(result)
        } catch (e: Throwable) {
            promise.fail(e)
        }
    }

}