package no.nav.aap.arenaoppslag.plugins

import com.fasterxml.jackson.core.JacksonException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
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
                is JacksonException -> {
                    logger.error("Feil ved deserialising. Se team-logs for stacktrace.")
                    teamLogs.error("Uhåndtert deserialisingsfeil", cause)

                    call.respond(
                        HttpStatusCode.InternalServerError,
                        FeilRespons("Feil i tjeneste: ${cause.message}"),
                    )
                }
                else -> {
                    logger.error("Uhåndtert feil. Se team-logs for stacktrace.")
                    teamLogs.error("Uhåndtert feil", cause)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        FeilRespons("Feil i tjeneste: ${cause.message}"),
                    )
                }
            }

        }
    }
}
