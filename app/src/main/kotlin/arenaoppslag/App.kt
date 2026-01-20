package arenaoppslag

import arenaoppslag.Metrics.prometheus
import arenaoppslag.aap.ArenaService
import arenaoppslag.aap.database.ArenaDatasource
import arenaoppslag.aap.database.MaksimumRepository
import arenaoppslag.aap.database.PeriodeRepository
import arenaoppslag.aap.database.PersonRepository
import arenaoppslag.aap.database.SakRepository
import arenaoppslag.plugins.contentNegotiation
import arenaoppslag.plugins.statusPages
import com.papsign.ktor.openapigen.model.info.ContactModel
import com.papsign.ktor.openapigen.model.info.InfoModel
import com.papsign.ktor.openapigen.route.apiRouting
import com.zaxxer.hikari.HikariDataSource
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureConfig
import no.nav.aap.komponenter.server.commonKtorModule
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import javax.sql.DataSource

val logger = LoggerFactory.getLogger("App")

object Metrics {
    val prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
}

@Suppress("MagicNumber")
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
    datasource: DataSource = ArenaDatasource.create(config.database)
) {
    statusPages()

    contentNegotiation()

    val azureConfig = AzureConfig(
        issuer = config.azure.issuer,
        jwksUri = config.azure.jwksUri,
        clientId = config.azure.clientId,
    )
    commonKtorModule(
        prometheus, azureConfig, infoModel = InfoModel(
            title = "aap-arenaoppslag",
            description = "aap-arenaoppslag tilbyr et internt API for henting av AAP-data fra Arena. \n" +
                    "Bruker Azure til autentisering.",
            contact = ContactModel(
                name = "Team AAP",
                url = "https://github.com/navikt/aap-arenaoppslag",
            )
        )
    )

    routing {
        actuator(prometheus)

        authenticate {
            // Bruker ikke RepositoryRegistry fra Kelvin-komponenter fordi vi er på Oracle DB her,
            // med annet opplegg for parameterized queries
            val periodeRepository = PeriodeRepository(datasource)
            val personRepository = PersonRepository(datasource)
            val maksimumRepository = MaksimumRepository(datasource)
            val sakRepository = SakRepository(datasource)
            val arenaService = ArenaService(personRepository, maksimumRepository, periodeRepository, sakRepository)
            apiRouting {
                route("/intern") {
                    perioder(arenaService)
                    person(arenaService)
                    maksimum(arenaService)
                    saker(arenaService)
                }
            }
        }
    }

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

private fun CallLoggingConfig.doLogCall() {
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
