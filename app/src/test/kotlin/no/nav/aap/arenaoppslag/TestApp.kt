package no.nav.aap.arenaoppslag

import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.aap.arenaoppslag.database.ArenaDatasource
import no.nav.aap.arenaoppslag.util.Fakes
import no.nav.aap.arenaoppslag.util.port
import org.flywaydb.core.Flyway

fun main() {
    val fakes = Fakes()

    val dataSource: HikariDataSource = ArenaDatasource.create(TestConfig.oracleH2)
    // Initialize database with schema and test data using the same initializer as tests
    Flyway.configure().dataSource(dataSource)
        .locations("flyway/common", "flyway/minimumtest", "flyway/common", "flyway/dsop").load()
        .apply {
            migrate()
            println("Testdatabase klar, url=${dataSource.jdbcUrl}")
        }

    val config = TestConfig.default(fakes)
    println("Azure port: ${fakes.azure.port()}")

    embeddedServer(Netty, port = 8080) {
        server(
            config = config,
        )
        module(dataSource)
    }.start(wait = true)
}

private fun Application.module(dataSource: HikariDataSource) {
    monitor.subscribe(ApplicationStopped) { application ->
        application.environment.log.info("Testserver har stoppet")
        dataSource.close()
        application.monitor.unsubscribe(ApplicationStopped) {}
    }
}
