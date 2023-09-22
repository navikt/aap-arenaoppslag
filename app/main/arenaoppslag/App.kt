package arenaoppslag

import arenaoppslag.fellesordning.FellesordningResponse
import arenaoppslag.fellesordning.FellesordningRequest
import arenaoppslag.arenamodell.Vedtak
import arenaoppslag.fellesordning.FelleordningRepo
import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
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
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.aap.ktor.config.loadConfig
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import java.net.InetSocketAddress
import java.net.ProxySelector
import java.net.URI
import java.util.concurrent.TimeUnit

private val logger = LoggerFactory.getLogger("main")

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::server).start(wait = true)
}

fun Application.server() {
    val prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val config = loadConfig<Config>()

    install(MicrometerMetrics) { registry = prometheus }

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            logger.error("Uhåndtert feil", cause)
            call.respondText(text = "Feil i tjeneste: ${cause.message}" , status = HttpStatusCode.InternalServerError)
        }
    }

    install(CallLogging) {
        level = Level.INFO
        format { call ->
            val status = call.response.status()
            val httpMethod = call.request.httpMethod.value
            val userAgent = call.request.headers["User-Agent"]
            val callId = call.request.header("x-callId") ?: call.request.header("nav-callId") ?: "ukjent"
            "Status: $status, HTTP method: $httpMethod, User agent: $userAgent, callId: $callId"
        }
        filter { call -> call.request.path().startsWith("/actuator").not() }
    }

    val proxyUri = URI.create(System.getenv("HTTP_PROXY"))
    val jwkProvider: JwkProvider = JwkProviderBuilder(URI(config.azure.jwksUri).toURL())
        .proxied(ProxySelector.of(InetSocketAddress(proxyUri.host, proxyUri.port)).select(URI(config.azure.jwksUri)).first())
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
                FellesordningResponse::class.java
            )
        }
    }

    val datasource = initDatasource(config.database)
    val felleordningRepo = FelleordningRepo(datasource)

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
                    val request = call.receive<FellesordningRequest>()
                    call.respond(felleordningRepo.hentGrunnInfoForAAPMotaker(request.personId, request.datoForOnsketUttakForAFP))
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