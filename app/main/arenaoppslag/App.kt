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
import io.ktor.server.plugins.callloging.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import javax.sql.DataSource

private val logger = LoggerFactory.getLogger("App")
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

    install(CallLogging) {
        level = Level.INFO
        format { call ->
            val status = call.response.status()
            val errorBody = if(status?.value != null && status.value > 499) ", ErrorBody: ${call.response}" else ""
            val httpMethod = call.request.httpMethod.value
            val userAgent = call.request.headers["User-Agent"]
            val callId = call.request.header("x-callid") ?: call.request.header("nav-callId") ?: "ukjent"
            val token = call.request.header("Authorization")
            val path = call.request.path()
            "Status: $status$errorBody, HTTP method: $httpMethod, User agent: $userAgent, Call id: $callId, Path: $path"
        }
        filter { call -> call.request.path().startsWith("/actuator").not() }
    }

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

        testroute(datasource)

    }
}
