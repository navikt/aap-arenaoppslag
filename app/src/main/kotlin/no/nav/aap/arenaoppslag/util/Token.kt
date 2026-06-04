package no.nav.aap.arenaoppslag.util

import io.ktor.http.HttpHeaders
import io.ktor.server.application.ApplicationCall
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import kotlin.text.split

data class OidcToken(val token: String)

public fun ApplicationCall.token(): OidcToken {
    val token: String = requireNotNull(this.request.headers[HttpHeaders.Authorization]).split(" ")[1]

    return OidcToken(token)
}