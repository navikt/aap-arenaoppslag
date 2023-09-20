package arenaoppslag

import arenaoppslag.fellesordning.FellesOrdningDTO
import arenaoppslag.fellesordning.FellesordningRequest
import arenaoppslag.modell.Vedtak
import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwk.RateLimitedJwkProvider
import com.auth0.jwk.UrlJwkProvider
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.engine.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.aap.ktor.config.loadConfig
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import java.net.URI
import java.util.concurrent.TimeUnit

private val secureLog = LoggerFactory.getLogger("secureLog")
private val logger = LoggerFactory.getLogger("main")

data class Config(
    val database: DbConfig,
    val azure: AzureConfig
)

data class DbConfig(
    val url: String,
    val username: String,
    val password: String
)

data class AzureConfig(
    val jwksUri: String,
    val issuer: String,
    val clientId: String
)

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::server).start(wait = true)
}

fun Application.server() {
    val prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val config = loadConfig<Config>()

    install(MicrometerMetrics) { registry = prometheus }

    Thread.currentThread().setUncaughtExceptionHandler { _, e -> secureLog.error("Uhåndtert feil", e) }

    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/actuator").not() }
    }

    val datasource = initDatasource(config.database)
    val repo = Repo(datasource)

    val jwkProvider: JwkProvider = JwkProviderBuilder(URI(config.azure.jwksUri).toURL())
        .cached(10, 24, TimeUnit.HOURS)
        .rateLimited(10, 1, TimeUnit.MINUTES)
        .build()
    install(Authentication) {
        jwt {
            verifier(jwkProvider, config.azure.issuer)
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized)
            }
            validate { credential ->
                if (credential.payload.audience.contains(config.azure.clientId)) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
        }
    }

    install(ContentNegotiation) {
        jackson {
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            registerSubtypes(
                Vedtak::class.java,
                FellesOrdningDTO::class.java
            )
        }
    }

    routing {
        route("/actuator") {
            get("/metrics") {
                call.respond(prometheus.scrape())
            }
            get("/live") {
                call.respond(HttpStatusCode.OK, "vedtak")
            }
            get("/ready") {
                call.respond(HttpStatusCode.OK, "vedtak")
            }
        }
        authenticate {
            route("/vedtak") {
                post {
                    logger.info("Mottar kall")
                    val request = call.receive<FellesordningRequest>()
                    logger.info("Melding $request mottatt")
                    try {
                        call.respond(repo.hentGrunnInfoForAAPMotaker(request.personId, request.datoForOnsketUttakForAFP))
                    } catch (e: Exception) {
                        logger.error("Feil ved henting", e)
                        call.respond(HttpStatusCode.InternalServerError, "Feil ved henting av info")
                    }
                }
            }
        }
    }
}

private fun initDatasource(dbConfig: DbConfig) = HikariDataSource(HikariConfig().apply {
    jdbcUrl = dbConfig.url
    username = dbConfig.username
    password = dbConfig.password
    maximumPoolSize = 3
    minimumIdle = 1
    initializationFailTimeout = 5000
    idleTimeout = 10001
    connectionTimeout = 1000
    maxLifetime = 30001
    driverClassName = "oracle.jdbc.OracleDriver"
    connectionTestQuery = "SELECT 1 FROM DUAL"
})