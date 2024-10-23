package arenaoppslag

import arenaoppslag.datasource.Hikari
import arenaoppslag.dsop.dsop
import arenaoppslag.ekstern.ekstern
import arenaoppslag.intern.intern
import arenaoppslag.plugins.authentication
import arenaoppslag.plugins.contentNegotiation
import arenaoppslag.plugins.statusPages
import io.ktor.http.HttpHeaders
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callid.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import java.util.UUID
import javax.sql.DataSource

private val logger = LoggerFactory.getLogger("App")

fun main() {
    Thread.currentThread()
        .setUncaughtExceptionHandler { _, e -> logger.error("Uhåndtert feil", e)}
    embeddedServer(Netty, port = 8080, module = Application::server).start(wait = true)
}

fun Application.server(
    config: Config = Config(),
    datasource: DataSource = Hikari.create(config.database)
) {
    val prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    install(CallId) {
        retrieveFromHeader(HttpHeaders.XCorrelationId)
        generate { UUID.randomUUID().toString() }
    }

    install(CallLogging) {
        callIdMdc("call-id")
        level = Level.INFO
        format { call ->
            val status = call.response.status()
            val errorBody =
                if (status?.value != null && status.value > 499) ", ErrorBody: ${call.response}" else ""
            val httpMethod = call.request.httpMethod.value
            val userAgent = call.request.headers["User-Agent"]
            val callId =
                call.request.header("x-callid") ?: call.request.header("nav-callId") ?: "ukjent"
            val path = call.request.path()
            "Status: $status$errorBody, HTTP method: $httpMethod, User agent: $userAgent, Call id: $callId, Path: $path"
        }
        filter { call -> call.request.path().startsWith("/actuator").not() }
    }

    install(MicrometerMetrics) { registry = prometheus }

    statusPages()

    authentication(config)

    contentNegotiation()

    routing {
        actuator(prometheus)

        authenticate {
            // TODO: fellesordningen og perioder er helt like. slå sammen eller beholde skille?
            dsop(datasource)
            intern(datasource)
            ekstern(datasource)
        }
    }
}
