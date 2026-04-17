package no.nav.aap.arenaoppslag

import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.aap.arenaoppslag.database.ArenaDatasource
import no.nav.aap.arenaoppslag.util.Fakes
import no.nav.aap.arenaoppslag.util.FakePdlGateway
import no.nav.aap.arenaoppslag.util.azure
import no.nav.aap.arenaoppslag.util.port
import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory

fun main() {
    val fakes = Fakes()
    val logger = LoggerFactory.getLogger("no.nav.aap.arenaoppslag.TestApp")
    val dataSource: HikariDataSource = ArenaDatasource.create(TestConfig.oracleH2)
    // Initialize database with schema and test data using the same initializer as tests
    Flyway.configure().dataSource(dataSource)
        .locations("flyway/common", "flyway/dsop", "flyway/minimumtest", "flyway/eksisterer").load()
        .apply {
            migrate()
            logger.info("Testdatabase klar, url=${dataSource.jdbcUrl}")
        }

    val config = TestConfig.default(fakes)
    logger.info("Azure port: ${fakes.azure.port()}")

    embeddedServer(Netty, port = 8087) {
        server(config = config, pdlGateway = FakePdlGateway())
        azure()
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
