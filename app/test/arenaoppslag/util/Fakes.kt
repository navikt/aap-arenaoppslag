package arenaoppslag.util

import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
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
    install(ContentNegotiation) { jackson() }
    routing {
        get("/jwks") {
            call.respond(HttpStatusCode.OK, AZURE_JWKS)
        }
    }
}

fun NettyApplicationEngine.port() = runBlocking { resolvedConnectors() }.first { it.type == ConnectorType.HTTP }.port
