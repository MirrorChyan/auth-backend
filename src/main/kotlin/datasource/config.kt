package datasource

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import config.Props
import org.ktorm.database.Database


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
}