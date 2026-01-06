package arenaoppslag

import arenaoppslag.Metrics.prometheus
import arenaoppslag.datasource.Hikari
import arenaoppslag.intern.intern
import arenaoppslag.plugins.authentication
import arenaoppslag.plugins.contentNegotiation
import arenaoppslag.plugins.statusPages
import com.zaxxer.hikari.HikariDataSource
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callid.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.micrometer.core.instrument.binder.logging.LogbackMetrics
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import java.util.*
import javax.sql.DataSource

val logger = LoggerFactory.getLogger("App")

object Metrics {
    val prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
}

fun main() {
    Thread.currentThread()
        .setUncaughtExceptionHandler { _, e -> logger.error("Uhåndtert feil", e) }
    embeddedServer(Netty, configure = {
        // Vi følger ktor sin metodikk for å regne ut tuning parametre som funksjon av parallellitet
        // https://github.com/ktorio/ktor/blob/3.3.2/ktor-server/ktor-server-core/common/src/io/ktor/server/engine/ApplicationEngine.kt#L30
        connectionGroupSize = AppConfig.ktorParallellitet / 2 + 1
        workerGroupSize = AppConfig.ktorParallellitet / 2 + 1
        callGroupSize = AppConfig.ktorParallellitet

        shutdownGracePeriod = AppConfig.shutdownGracePeriod.inWholeMilliseconds
        shutdownTimeout = AppConfig.shutdownTimeout.inWholeMilliseconds

        connector {
            port = 8080
        }
    }, module = Application::server).start(wait = true)
}

fun Application.server(
    config: AppConfig = AppConfig(),
    datasource: DataSource = Hikari.create(config.database)
) {
    install(CallId) {
        retrieveFromHeader(HttpHeaders.XCorrelationId)
        generate { UUID.randomUUID().toString() }
    }

    install(CallLogging) {
        callIdMdc("call-id")
        level = Level.INFO
        format { call ->
            val status = call.response.status()
            val errorBody = if (status?.isSuccess() == false) ", ErrorBody: ${call.response}" else ""
            val httpMethod = call.request.httpMethod.value
            val userAgent = call.request.userAgent()
            val callId = call.request.header("x-callid") ?: call.request.header("nav-callId") ?: "ukjent"
            val path = call.request.path()
            "Status: $status$errorBody, HTTP method: $httpMethod, User agent: $userAgent, Call id: $callId, Path: $path"
        }
        filter { call -> call.request.path().startsWith("/actuator").not() }
    }

    install(MicrometerMetrics) {
        meterBinders += LogbackMetrics()
        registry = prometheus
    }

    statusPages()

    authentication(config)

    contentNegotiation()

    routing {
        actuator(prometheus)

        authenticate {
            intern(datasource)
        }
    }

    monitor.subscribe(ApplicationStarted) { environment ->
        environment.log.info("ktor har startet opp.")
    }
    monitor.subscribe(ApplicationStopPreparing) { environment ->
        environment.log.info("ktor forbereder seg på å stoppe.")
    }
    monitor.subscribe(ApplicationStopping) { environment ->
        environment.log.info("ktor stopper nå å ta imot nye requester, " +
                "og lar mottatte requester kjøre frem til timeout.")
    }
    monitor.subscribe(ApplicationStopped) { environment ->
        environment.log.info("ktor har fullført nedstoppingen sin. " +
                "Eventuelle requester og annet arbeid som ikke ble fullført innen timeout ble avbrutt.")
        try {
            (datasource as? HikariDataSource)?.close() // en annen type i Test enn i Prod
        } catch (_: Exception) {
            // Ignorert
        }
    }
}
