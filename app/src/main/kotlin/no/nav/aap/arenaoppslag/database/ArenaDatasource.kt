package no.nav.aap.arenaoppslag.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.aap.arenaoppslag.DbConfig
import no.nav.aap.arenaoppslag.Metrics
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

internal object ArenaDatasource {

    @Suppress("MagicNumber")
    fun create(dbConfig: DbConfig): HikariDataSource =
        HikariDataSource(HikariConfig().apply {
            jdbcUrl = dbConfig.url
            username = dbConfig.username
            password = dbConfig.password
            driverClassName = dbConfig.driver
            initializationFailTimeout = 15.seconds.inWholeMilliseconds
            connectionTimeout = 5.seconds.inWholeMilliseconds
            keepaliveTime = 2.minutes.inWholeMilliseconds
            maxLifetime = 5.minutes.inWholeMilliseconds
            connectionTestQuery = "SELECT 1 FROM DUAL"
            // performance:
            // do not set minimumIdle, it defaults to maximumPoolSize, matching hikaricp performance recommendations.
            // idleTimeout is not relevant in this case and is omitted.
            isReadOnly = true
            isAutoCommit = true // performance optimization for read-only operations, saves transaction work
            metricRegistry = Metrics.prometheus

            // By default, there is no read timeout, and an application might hang indefinitely
            // in case of a network failure.
            addDataSourceProperty(
                "oracle.jdbc.ReadTimeout",
                5.minutes.inWholeMilliseconds.toString()
            )
        })
}

fun <T : Any> ResultSet.map(block: (ResultSet) -> T): List<T> =
    sequence {
        while (next()) yield(block(this@map))
    }.toList()

fun Connection.createParameterizedQuery(queryString: String): PreparedStatement {
    val query = prepareStatement(queryString)
    query.queryTimeout = 300 // set a timeout in seconds, to avoid long running queries
    return query
}
