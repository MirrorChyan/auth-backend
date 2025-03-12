package config

import cache.evictAll
import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.pubsub.RedisPubSubAdapter
import org.slf4j.LoggerFactory


private val log = LoggerFactory.getLogger("RdsKt")!!

object RDS {
    private val client = run {
        RedisClient.create(Props.Redis.url)
    }
    private val single = run {
        val connection: StatefulRedisConnection<String, String> = client.connect()
        connection.async()
    }

    private const val TOPIC = "auth"

    fun subscribe() {
        val sub = client.connectPubSub()

        sub.addListener(object : RedisPubSubAdapter<String, String>() {
            override fun message(channel: String, message: String) {
                if (channel == TOPIC) {
                    log.warn("evict all cache")
                    evictAll()
                }
            }

        })
        sub.async().subscribe(TOPIC)
    }

    fun get(): RedisAsyncCommands<String, String> {
        return single
    }
}