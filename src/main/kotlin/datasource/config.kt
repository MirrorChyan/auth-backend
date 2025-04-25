package datasource

import cache.doSubscribeEvictEvent
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import config.Props
import metrics.initMetrics
import org.ktorm.database.Database
import stat.StatHelper


lateinit var DB: Database

fun config() {
    DB = HikariDataSource(HikariConfig().apply {
        jdbcUrl = Props.Datasource.url
        username = Props.Datasource.username
        password = Props.Datasource.password
        driverClassName = Props.Datasource.driver
    }).run {
        Database.connect(this)
    }

//    initMetrics()
    doSubscribeEvictEvent()
    StatHelper.poll()
}