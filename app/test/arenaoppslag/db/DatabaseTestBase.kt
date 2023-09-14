package arenaoppslag.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.BeforeEach
import org.testcontainers.containers.OracleContainer
import javax.sql.DataSource

internal object InitTestDatabase {
    private val container: OracleContainer = OracleContainer("gvenzl/oracle-xe:18.4.0-slim-faststart")
    private val flyway: Flyway

    internal val dataSource: DataSource

    init {
        container.start()
        dataSource = HikariDataSource(HikariConfig().apply {
            jdbcUrl = container.jdbcUrl
            username = container.username
            password = container.password
            maximumPoolSize = 3
            minimumIdle = 1
            initializationFailTimeout = 5000
            idleTimeout = 10001
            connectionTimeout = 1000
            maxLifetime = 30001
        })

        flyway = Flyway.configure().dataSource(dataSource).locations("flyway").load().apply { migrate() }
    }
}

internal abstract class DatabaseTestBase {
    @BeforeEach
    fun clearTables() {
        InitTestDatabase.dataSource.connection.use { connection ->
            connection.prepareStatement("TRUNCATE TABLE person, sak, vedtak, vedtakfakta").use { preparedStatement ->
                preparedStatement.execute()
            }
        }
    }
}
