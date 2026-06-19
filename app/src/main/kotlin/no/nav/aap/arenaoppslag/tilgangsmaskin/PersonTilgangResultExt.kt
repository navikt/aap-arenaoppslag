package no.nav.aap.arenaoppslag.tilgangsmaskin

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

suspend fun RoutingContext.medTilgangSjekket(
    tilgang: PersonTilgangResult,
    onAccessDenied: suspend RoutingContext.() -> Unit = { call.respond(HttpStatusCode.Forbidden) },
    onNotFound: suspend RoutingContext.() -> Unit = {
        call.respond(HttpStatusCode.NotFound, "Fant ikke personen i Arena")
    },
    onGranted: suspend RoutingContext.(PersonTilgangResult.Granted) -> Unit,
) {
    when (tilgang) {
        is PersonTilgangResult.AccessDenied -> onAccessDenied()
        is PersonTilgangResult.NotFound -> onNotFound()
        is PersonTilgangResult.Granted -> onGranted(tilgang)
    }
}

