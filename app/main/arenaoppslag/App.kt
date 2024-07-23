package arenaoppslag

import arenaoppslag.datasource.Hikari
import arenaoppslag.dsop.dsop
import arenaoppslag.fellesordningen.fellesordningen
import arenaoppslag.perioder.perioder
import arenaoppslag.plugins.authentication
import arenaoppslag.plugins.contentNegotiation
import arenaoppslag.plugins.statusPages
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.sql.DataSource

private val secureLog: Logger = LoggerFactory.getLogger("secureLog")

fun main() {
    Thread.currentThread().setUncaughtExceptionHandler { _, e -> secureLog.error("Uhåndtert feil", e) }
    embeddedServer(Netty, port = 8080, module = Application::server).start(wait = true)
}

fun Application.server(
    config: Config = Config(),
    datasource: DataSource = Hikari.create(config.database)
) {
    val prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    install(MicrometerMetrics) { registry = prometheus }

    statusPages(secureLog)

    authentication(config)

    contentNegotiation()

    routing {
        actuator(prometheus)

        authenticate {
            // TODO: fellesordningen og perioder er helt like. slå sammen eller beholde skille?
            fellesordningen(datasource)
            perioder(datasource)
            dsop(datasource)
        }

    }
}
