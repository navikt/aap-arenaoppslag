package no.nav.aap.arenaoppslag.tilgangsmaskin

import io.ktor.server.routing.*

suspend fun RoutingContext.medTilgangKontrollert(
    tilgang: PersonTilgangResultat,
    onAccessDenied: suspend RoutingContext.(PersonTilgangResultat.AccessDenied) -> Unit,
    onNotFound: suspend RoutingContext.() -> Unit,
    onGranted: suspend RoutingContext.(PersonTilgangResultat.Granted) -> Unit,
) {
    when (tilgang) {
        is PersonTilgangResultat.AccessDenied -> onAccessDenied(tilgang)
        is PersonTilgangResultat.NotFound -> onNotFound()
        is PersonTilgangResultat.Granted -> onGranted(tilgang)
    }
}

suspend fun RoutingContext.medTilgangKontrollert(
    tilgang: SakTilgangResultat,
    onAccessDenied: suspend RoutingContext.(SakTilgangResultat.AccessDenied) -> Unit,
    onNotFound: suspend RoutingContext.() -> Unit,
    onGranted: suspend RoutingContext.(SakTilgangResultat.Granted) -> Unit,
) {
    when (tilgang) {
        is SakTilgangResultat.AccessDenied -> onAccessDenied(tilgang)
        is SakTilgangResultat.NotFound -> onNotFound()
        is SakTilgangResultat.Granted -> onGranted(tilgang)
    }
}
