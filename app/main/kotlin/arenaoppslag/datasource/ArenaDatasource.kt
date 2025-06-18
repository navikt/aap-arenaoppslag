package arenaoppslag.datasource

import arenaoppslag.DbConfig
import arenaoppslag.prometheus
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.ResultSet
import javax.sql.DataSource

internal object Hikari {
    fun create(dbConfig: DbConfig): DataSource =
        HikariDataSource(HikariConfig().apply {
            jdbcUrl = dbConfig.url
            username = dbConfig.username
            password = dbConfig.password
            maximumPoolSize = 3
            minimumIdle = 1
            initializationFailTimeout = 5000
            idleTimeout = 10001
            connectionTimeout = 1000
            maxLifetime = 30001
            driverClassName = dbConfig.driver
            connectionTestQuery = "SELECT 1 FROM DUAL"
            metricRegistry = prometheus
        })
}

fun <T : Any> ResultSet.map(block: (ResultSet) -> T): List<T> =
    sequence {
        while (next()) yield(block(this@map))
    }.toList()
