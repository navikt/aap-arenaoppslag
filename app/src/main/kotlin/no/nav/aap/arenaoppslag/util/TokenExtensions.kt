package no.nav.aap.arenaoppslag.util

import io.ktor.server.application.*
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken

fun ApplicationCall.token(): OidcToken {
    val raw = request.headers["Authorization"]
        ?.removePrefix("Bearer ")
        ?: error("Authorization header mangler — kall bare token() fra autentiserte routes")
    return OidcToken(raw)
}
