package no.nav.aap.arenaoppslag.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.aap.arenaoppslag.DbConfig
import no.nav.aap.arenaoppslag.TestConfig
import org.flywaydb.core.Flyway
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.sql.DataSource
import kotlin.time.Duration.Companion.seconds

abstract class H2TestBase(private vararg val migrationLocations: String = emptyArray()) {

    protected val h2: DataSource by lazy {
        getOrCreateDatabaseForTestClass(this::class.java, migrationLocations)
    }

    companion object {
        private val dbCounter = AtomicInteger(0)
        private val databases = ConcurrentHashMap<Class<*>, DataSource>()

        private fun getOrCreateDatabaseForTestClass(testClass: Class<*>, additionalLocations: Array<out String>): DataSource {
            return databases.computeIfAbsent(testClass) { clazz ->
                createAndInitializeDatabase(clazz.simpleName, additionalLocations)
            }
        }

        private fun createAndInitializeDatabase(testClassName: String, additionalLocations: Array<out String>): DataSource {
            val dbId = dbCounter.incrementAndGet()
            val dbName = "test_${testClassName}_${dbId}"

            val config = DbConfig(
                username = TestConfig.oracleH2InMem.username,
                password = TestConfig.oracleH2InMem.password,
                url = "jdbc:h2:mem:$dbName;MODE=Oracle;DB_CLOSE_DELAY=-1;TRACE_LEVEL_SYSTEM_OUT=1",
                driver = TestConfig.oracleH2InMem.driver
            )

            // Create a non-read-only datasource for tests (unlike ArenaDatasource which is read-only)
            val dataSource = HikariDataSource(HikariConfig().apply {
                jdbcUrl = config.url
                username = config.username
                password = config.password
                driverClassName = config.driver
                isAutoCommit = true
            })

            // Always include flyway/common as base, plus any additional locations
            val allLocations = arrayOf("flyway/common") + additionalLocations

            Flyway.configure()
                .dataSource(dataSource)
                .locations(*allLocations)
                .load()
                .apply { migrate() }

            return dataSource
        }
    }

}


