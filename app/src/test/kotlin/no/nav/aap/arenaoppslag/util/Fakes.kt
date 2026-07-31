package no.nav.aap.arenaoppslag.util

import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.ConnectorType
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.runBlocking

class Fakes : AutoCloseable {
    internal val texas = embeddedServer(Netty, port = 0, module = Application::texas).apply { start() }

    init {
        // Texas
        System.setProperty("nais.token.endpoint", "http://localhost:${texas.port()}/token")
        System.setProperty("nais.token.exchange.endpoint", "http://localhost:${texas.port()}/token/exchange")
        System.setProperty("nais.token.introspection.endpoint", "http://localhost:${texas.port()}/introspect")
    }

    override fun close() {
        texas.stop(0L, 0L)
    }
}

fun Application.texas() {
    install(ContentNegotiation) {
        jackson()
    }

    routing {
        post("/token") {
            val token = AzureTokenGen("azure", "no/nav/aap/arenaoppslag").generate()
            call.respond(TestToken(access_token = token))
        }

        post("/introspect") {
            call.respond(mapOf("active" to true))
        }
    }
}

@Suppress("PropertyName", "ConstructorParameterNaming")
data class TestToken(
    val access_token: String,
    val refresh_token: String = "very.secure.token",
    val id_token: String = "very.secure.token",
    val token_type: String = "token-type",
    val scope: String? = null,
    val expires_in: Int = 3599,
)

fun EmbeddedServer<*, *>.port(): Int =
    runBlocking { this@port.engine.resolvedConnectors() }
        .first { it.type == ConnectorType.HTTP }
        .port
