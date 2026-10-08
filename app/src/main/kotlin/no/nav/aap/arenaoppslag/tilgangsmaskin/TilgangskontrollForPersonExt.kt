package no.nav.aap.arenaoppslag.tilgangsmaskin

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

suspend fun RoutingContext.medTilgangKontrollert(
    tilgang: PersonTilgangResultat,
    onAccessDenied: suspend RoutingContext.() -> Unit = { call.respond(HttpStatusCode.Forbidden) },
    onNotFound: suspend RoutingContext.() -> Unit = {
        call.respond(HttpStatusCode.NotFound, "Fant ikke personen i Arena")
    },
    onGranted: suspend RoutingContext.(PersonTilgangResultat.Granted) -> Unit,
) {
    when (tilgang) {
        is PersonTilgangResultat.AccessDenied -> onAccessDenied()
        is PersonTilgangResultat.NotFound -> onNotFound()
        is PersonTilgangResultat.Granted -> onGranted(tilgang)
    }
}

suspend fun RoutingContext.medTilgangKontrollert(
    tilgang: SakTilgangResultat,
    onAccessDenied: suspend RoutingContext.() -> Unit = { call.respond(HttpStatusCode.Forbidden) },
    onNotFound: suspend RoutingContext.() -> Unit = {
        call.respond(HttpStatusCode.NotFound, "Fant ikke saken i Arena")
    },
    onGranted: suspend RoutingContext.(SakTilgangResultat.Granted) -> Unit,
) {
    when (tilgang) {
        is SakTilgangResultat.AccessDenied -> onAccessDenied()
        is SakTilgangResultat.NotFound -> onNotFound()
        is SakTilgangResultat.Granted -> onGranted(tilgang)
    }
}
