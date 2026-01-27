package no.nav.aap.arenaoppslag

import com.zaxxer.hikari.HikariDataSource
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callid.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.micrometer.core.instrument.binder.logging.LogbackMetrics
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.arenaoppslag.Metrics.prometheus
import no.nav.aap.arenaoppslag.database.ArenaDatasource
import no.nav.aap.arenaoppslag.database.HistorikkRepository
import no.nav.aap.arenaoppslag.database.MaksimumRepository
import no.nav.aap.arenaoppslag.database.PeriodeRepository
import no.nav.aap.arenaoppslag.database.PersonRepository
import no.nav.aap.arenaoppslag.database.SakRepository
import no.nav.aap.arenaoppslag.plugins.MdcKeys
import no.nav.aap.arenaoppslag.plugins.authentication
import no.nav.aap.arenaoppslag.plugins.bruker
import no.nav.aap.arenaoppslag.plugins.statusPages
import no.nav.aap.komponenter.json.DefaultJsonMapper
import org.slf4j.LoggerFactory
import java.util.*
import javax.sql.DataSource

val logger = LoggerFactory.getLogger("App")

object Metrics {
    val prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
}

@Suppress("MagicNumber")
fun main() {
    Thread.currentThread().setUncaughtExceptionHandler { _, e -> logger.error("Uhåndtert feil", e) }
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
    config: AppConfig = AppConfig(), datasource: DataSource = ArenaDatasource.create(config.database)
) {
    statusPages()

    install(MicrometerMetrics) {
        registry = prometheus
        meterBinders += LogbackMetrics()
    }
    install(ContentNegotiation) {
        register(
            ContentType.Application.Json,
            JacksonConverter(objectMapper = DefaultJsonMapper.objectMapper(), true)
        )
    }
    install(CallLogging) {
        callIdMdc(MdcKeys.CallId)
        // For å unngå rare tegn i loggene
        disableDefaultColors()
        filter { call -> call.request.path().startsWith("/actuator").not() }
        mdc(MdcKeys.User) { call -> runCatching { call.bruker().ident }.getOrNull() }
    }
    install(CallId) {
        retrieveFromHeader(HttpHeaders.XCorrelationId)
        generate { UUID.randomUUID().toString() }
    }

    authentication(config)

    routing(datasource)

    monitor.subscribe(ApplicationStarted) { environment ->
        environment.log.info("ktor har startet opp.")
    }
    monitor.subscribe(ApplicationStopPreparing) { environment ->
        environment.log.info("ktor forbereder seg på å stoppe.")
    }
    monitor.subscribe(ApplicationStopping) { environment ->
        environment.log.info(
            "ktor stopper nå å ta imot nye requester, " +
                    "og lar mottatte requester kjøre frem til timeout."
        )
    }
    monitor.subscribe(ApplicationStopped) { environment ->
        environment.log.info(
            "ktor har fullført nedstoppingen sin. " +
                    "Eventuelle requester og annet arbeid som ikke ble fullført innen timeout ble avbrutt."
        )
        try {
            (datasource as? HikariDataSource)?.close() // en annen type i Test enn i Prod
        } catch (_: Exception) {
            // Ignorert
        }
    }
}

private fun Application.routing(datasource: DataSource) {
    routing {
        actuator(prometheus)

        authenticate {
            // Bruker ikke RepositoryRegistry fra Kelvin-komponenter fordi vi er på Oracle DB her,
            // med annet opplegg for parameterized queries
            val periodeRepository = PeriodeRepository(datasource)
            val personRepository = PersonRepository(datasource)
            val maksimumRepository = MaksimumRepository(datasource)
            val sakRepository = SakRepository(datasource)
            val historikkRepository = HistorikkRepository(datasource)
            val arenaService = ArenaService(
                personRepository, maksimumRepository, periodeRepository,
                sakRepository, historikkRepository
            )
            route("/intern") {
                perioder(arenaService)
                person(arenaService)
                maksimum(arenaService)
                saker(arenaService)
            }
        }
    }
}
