package no.nav.aap.arenaoppslag.plugins

import com.fasterxml.jackson.core.JacksonException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import no.nav.aap.komponenter.httpklient.exception.ApiException
import no.nav.aap.komponenter.httpklient.exception.InternfeilException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("App")
private val teamLogs: Logger = LoggerFactory.getLogger("team-logs")

data class FeilRespons(
    val melding: String
)

fun Application.statusPages() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            when (cause) {
                is InternfeilException -> {
                    logger.error(cause.cause?.message ?: cause.message)
                    call.respondWithError(cause)
                }

                is ApiException -> {
                    logger.warn(cause.message, cause)
                    call.respondWithError(cause)
                }

                is JacksonException -> {
                    logger.error("Uhåndtert deserialisingsfeil", cause)

                    call.respond(
                        HttpStatusCode.InternalServerError,
                        FeilRespons("Feil i tjeneste: ${cause.message}"),
                    )
                }

                else -> {
                    logger.error("Uhåndtert feil", cause)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        FeilRespons("Feil i tjeneste: ${cause.message}"),
                    )
                }
            }

        }
    }
}

private suspend fun ApplicationCall.respondWithError(exception: ApiException) {
    respond(
        exception.status,
        exception.tilApiErrorResponse()
    )
}
