package arenaoppslag.datasource

import arenaoppslag.DbConfig
import arenaoppslag.prometheus
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.ResultSet
import java.util.Properties
import javax.sql.DataSource


internal object Hikari {
    private val postgresConfig = Properties().apply {
        put("tcpKeepAlive", true) // kreves av Hikari

        put("socketTimeout", 300) // sekunder, makstid for overføring av svaret fra db
        put("statement_timeout", 300_000) // millisekunder, makstid for db til å utføre spørring

        put("logUnclosedConnections", true) // vår kode skal lukke alle connections
        put("logServerErrorDetail", false) // ikke lekk person-data fra queries etc til logger ved feil

        put("assumeMinServerVersion", "16.0") // raskere oppstart av driver
    }

    fun create(dbConfig: DbConfig): DataSource =
        HikariDataSource(HikariConfig().apply {
            jdbcUrl = dbConfig.url
            username = dbConfig.username
            password = dbConfig.password
            minimumIdle = 1
            initializationFailTimeout = 5000
            idleTimeout = 10001
            connectionTimeout = 1000
            maxLifetime = 30001
            driverClassName = dbConfig.driver
            connectionTestQuery = "SELECT 1 FROM DUAL"
            metricRegistry = prometheus
            dataSourceProperties = postgresConfig
            maximumPoolSize = 10
        })
}

fun <T : Any> ResultSet.map(block: (ResultSet) -> T): List<T> =
    sequence {
        while (next()) yield(block(this@map))
    }.toList()
