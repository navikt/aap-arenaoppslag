package arenaoppslag.aap.database

import arenaoppslag.DbConfig
import arenaoppslag.Metrics
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.ResultSet
import java.util.concurrent.TimeUnit
import javax.sql.DataSource
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

internal object ArenaDatasource {

    @Suppress("MagicNumber")
    fun create(dbConfig: DbConfig): DataSource =
        HikariDataSource(HikariConfig().apply {
            jdbcUrl = dbConfig.url
            username = dbConfig.username
            password = dbConfig.password
            driverClassName = dbConfig.driver
            initializationFailTimeout = 5.seconds.inWholeMilliseconds
            connectionTimeout = 1.seconds.inWholeMilliseconds
            keepaliveTime = 2.minutes.inWholeMilliseconds
            maxLifetime = 5.minutes.inWholeMilliseconds
            connectionTestQuery = "SELECT 1 FROM DUAL"
            // performance:
            // do not set minimumIdle, it defaults to maximumPoolSize, matching hikaricp performance recommendations.
            // idleTimeout is not relevant in this case and is omitted.
            isReadOnly = true
            isAutoCommit = true // performance optimization for read-only operations, saves transaction work
            metricRegistry = Metrics.prometheus
        })
}

fun <T : Any> ResultSet.map(block: (ResultSet) -> T): List<T> =
    sequence {
        while (next()) yield(block(this@map))
    }.toList()
