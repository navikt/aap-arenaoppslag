package no.nav.aap.arenaoppslag.plugins

import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal

@Suppress("MayBeConstant")
internal object MdcKeys {
    val CallId: String = "callId"
    val User: String = "x_user"
}

internal data class Bruker(public val ident: String)

internal fun ApplicationCall.bruker(): Bruker {
    val navIdent = principal<JWTPrincipal>()?.getClaim("NAVident", String::class)
    if (navIdent == null) {
        error("NAVident mangler i AzureAD claims")
    }
    return Bruker(navIdent)
}
