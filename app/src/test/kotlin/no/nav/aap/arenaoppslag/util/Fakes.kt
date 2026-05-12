package no.nav.aap.arenaoppslag.util

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking

class Fakes : AutoCloseable {
    val azure = embeddedServer(Netty, port = 0, module = Application::azure).apply { start() }

    override fun close() {
        azure.stop(0L, 0L)
    }
}

fun Application.azure() {
    routing {
        post("/token") {
            val token = AzureTokenGen("azure", "no/nav/aap/arenaoppslag").generate()
            call.respond(TestToken(access_token = token))
        }
        get("/jwks") {
            call.respond(AZURE_JWKS)
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
