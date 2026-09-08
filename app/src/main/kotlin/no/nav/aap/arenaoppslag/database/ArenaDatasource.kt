package no.nav.aap.arenaoppslag.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import no.nav.aap.arenaoppslag.DbConfig
import no.nav.aap.arenaoppslag.Metrics
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import javax.sql.DataSource
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

// Speiler HikariCP sin egen default maximumPoolSize, brukt som fallback for datasources
// (f.eks. H2 i tester) som ikke er HikariDataSource.
private const val DEFAULT_MAKS_POOLSTØRRELSE = 10

internal object ArenaDatasource {
    @Suppress("MagicNumber")

    fun create(dbConfig: DbConfig): HikariDataSource = HikariDataSource(HikariConfig().apply {
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
            "oracle.jdbc.ReadTimeout", 5.minutes.inWholeMilliseconds.toString()
        )
    })
}

fun <T : Any> ResultSet.map(block: (ResultSet) -> T): List<T> = sequence {
    while (next()) yield(block(this@map))
}.toList()

// getInt returnerer 0 for NULL-kolonner — vi bruker wasNull() for å skille null fra 0
fun ResultSet.getIntOrNull(columnLabel: String): Int? {
    val value = getInt(columnLabel)
    return if (wasNull()) null else value
}

// getDouble returnerer 0.0 for NULL-kolonner — vi bruker wasNull() for å skille null fra 0.0
fun ResultSet.getDoubleOrNull(columnLabel: String): Double? {
    val value = getDouble(columnLabel)
    return if (wasNull()) null else value
}

@Suppress("MagicNumber")
fun Connection.createParameterizedQuery(queryString: String): PreparedStatement {
    val query = prepareStatement(queryString)
    query.queryTimeout = 300 // set a timeout in seconds, to avoid long running queries
    return query
}

// Blokkerende JDBC-kall må avlastes fra Ktor/Netty sine event loop-tråder (se AppConfig.ktorParallellitet),
// ellers vil ett tregt Oracle-kall blokkere HELE applikasjonen for alle andre samtidige kall. Dispatcheren
// begrenses til HikariCP sin maximumPoolSize — flere samtidige DB-kall enn det gir uansett ingen nytte,
// de vil bare vente på en ledig connection.
fun DataSource.tilDbDispatcher(): CoroutineDispatcher {
    val poolstørrelse = (this as? HikariDataSource)?.maximumPoolSize ?: DEFAULT_MAKS_POOLSTØRRELSE
    return Dispatchers.IO.limitedParallelism(poolstørrelse)
}
