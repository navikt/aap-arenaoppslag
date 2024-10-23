package arenaoppslag.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("App")
private val secureLog: Logger = LoggerFactory.getLogger("secureLog")

fun Application.statusPages() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            logger.error("Uhåndtert feil. Se sikker logg for stack trace.")
            secureLog.error("Uhåndtert feil", cause)
            call.respondText(
                text = "Feil i tjeneste: ${cause.message}",
                status = HttpStatusCode.InternalServerError
            )
        }
    }
}