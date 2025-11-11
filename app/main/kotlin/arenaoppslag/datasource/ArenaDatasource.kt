package arenaoppslag.datasource

import arenaoppslag.DbConfig
import arenaoppslag.Metrics
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
            driverClassName = dbConfig.driver
            initializationFailTimeout = 5000
            idleTimeout = 10001
            connectionTimeout = 1000
            maxLifetime = 30001
            connectionTestQuery = "SELECT 1 FROM DUAL"
            // performance:
            // do not set minimumIdle, it defaults to maximumPoolSize, matching hikaricp performance recommendations
            isReadOnly = true
            isAutoCommit = true // performance optimization for read-only operations, saves transaction work
            metricRegistry = Metrics.prometheus
        })
}

fun <T : Any> ResultSet.map(block: (ResultSet) -> T): List<T> =
    sequence {
        while (next()) yield(block(this@map))
    }.toList()
