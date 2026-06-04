package no.nav.aap.arenaoppslag.util

import io.ktor.server.application.*
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken

/**
 * Extracts the raw Bearer token from the Authorization header and wraps it as an [OidcToken].
 * Throws if the header is missing — should only be called inside an authenticated route.
 */
fun ApplicationCall.token(): OidcToken {
    val raw = request.headers["Authorization"]
        ?.removePrefix("Bearer ")
        ?: error("Authorization header mangler — kall bare token() fra autentiserte routes")
    return OidcToken(raw)
}
