package arenaoppslag

import arenaoppslag.fellesordningen.VedtakResponse
import arenaoppslag.arenamodell.Vedtak
import arenaoppslag.datasource.Hikari
import arenaoppslag.dsop.dsop
import arenaoppslag.fellesordningen.fellesordningen
import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
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
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import java.net.InetSocketAddress
import java.net.ProxySelector
import java.net.URI
import java.util.concurrent.TimeUnit

private val secureLog: Logger = LoggerFactory.getLogger("secureLog")

fun main() {
    Thread.currentThread().setUncaughtExceptionHandler { _, e -> secureLog.error("Uhåndtert feil", e) }
    embeddedServer(Netty, port = 8080, module = Application::server).start(wait = true)
}

fun Application.server() {
    val prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val config = Config()

    install(MicrometerMetrics) { registry = prometheus }

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            secureLog.error("Uhåndtert feil", cause)
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

    val proxyUri = URI.create(config.proxyUrl)
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
                VedtakResponse::class.java
            )
        }
    }

    val datasource = Hikari.create(config.database)

    routing {
        route("/actuator") {
            get("/metrics") {
                call.respond(prometheus.scrape())
            }
            get("/live") {
                call.respond(HttpStatusCode.OK, "arenaoppslag")
            }
            get("/ready") {
                call.respond(HttpStatusCode.OK, "arenaoppslag")
            }
        }

        authenticate {
            fellesordningen(datasource)
            dsop(datasource)
        }
    }
}
