package no.nav.aap.arenaoppslag.plugins

import no.nav.aap.arenaoppslag.AppConfig
import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import java.net.InetSocketAddress
import java.net.ProxySelector
import java.net.URI
import java.util.concurrent.TimeUnit

@Suppress("MagicNumber")
fun Application.authentication(config: AppConfig) {
    val proxyUri = URI.create(config.proxyUrl)

    val jwkProvider: JwkProvider = if(config.enableProxy) {
        JwkProviderBuilder(URI(config.azure.jwksUri).toURL())
            .proxied(
                ProxySelector.of(InetSocketAddress(proxyUri.host, proxyUri.port))
                    .select(URI(config.azure.jwksUri)).first()
            )
            .cached(10, 24, TimeUnit.HOURS)
            .rateLimited(10, 1, TimeUnit.MINUTES)
            .build()
    } else {
        JwkProviderBuilder(URI(config.azure.jwksUri).toURL())
            .cached(10, 24, TimeUnit.HOURS)
            .rateLimited(10, 1, TimeUnit.MINUTES)
            .build()
    }

    install(Authentication) {
        jwt {
            verifier(jwkProvider, config.azure.issuer)
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized)
            }
            validate { credential ->
                if (credential.payload.audience.contains(config.azure.clientId)) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
        }
    }
}

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
