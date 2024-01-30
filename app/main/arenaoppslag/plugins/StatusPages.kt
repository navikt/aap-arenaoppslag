package arenaoppslag.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import org.slf4j.Logger

fun Application.statusPages(logger: Logger) {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            logger.error("Uhåndtert feil", cause)
            call.respondText(text = "Feil i tjeneste: ${cause.message}" , status = HttpStatusCode.InternalServerError)
        }
    }
}