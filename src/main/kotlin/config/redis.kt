package config

import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisAsyncCommands

object RDS {
    private val single = run {
        val client: RedisClient = RedisClient.create(Props.Redis.url)
        val connection: StatefulRedisConnection<String, String> = client.connect()
        connection.async()
    }

    fun get(): RedisAsyncCommands<String, String> {
        return single
    }
}