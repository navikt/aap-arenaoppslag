package no.nav.aap.arenaoppslag

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.aap.arenaoppslag.modeller.PersonIdentifikator
import no.nav.aap.arenaoppslag.tilgang.TilgangGatewayImpl
import no.nav.aap.arenaoppslag.tilgang.TilgangHttpClient
import no.nav.aap.arenaoppslag.util.AzureTokenGen
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.TokenProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TilgangAvvistApiTest {
    @Test
    fun `200 OK med tilgang false betyr ingen tilgang`(): Unit = runBlocking {
        val scope = "api://test.aap.tilgang/.default"
        val konfig = mapOf(
            "INTEGRASJON_TILGANG_URL" to "http://tilgang",
            "INTEGRASJON_TILGANG_SCOPE" to scope,
        )
        val tidligereKonfig = konfig.mapValues { (key, _) -> System.getProperty(key) }
        val brukerToken = OidcToken(AzureTokenGen("issuer", "arenaoppslag").generate())
        val oboToken = OidcToken(AzureTokenGen("issuer", "tilgang").generate())
        val tokenProvider = mockk<TokenProvider>()
        every { tokenProvider.getToken(scope, brukerToken) } returns oboToken
        val engine = MockEngine {
            respond(
                """{"tilgang":false}""",
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        try {
            konfig.forEach { (key, value) -> System.setProperty(key, value) }
            TilgangGatewayImpl(TilgangHttpClient.lagHttpClient(engine), tokenProvider).use { gateway ->
                val resultat = gateway.harTilgangTilPerson(PersonIdentifikator("12312312312"), brukerToken)

                assertThat(resultat.tilgang).isFalse()
                assertThat(resultat.tilgangIKontekst).isNull()
                assertThat(engine.requestHistory).hasSize(1)
            }
        } finally {
            engine.close()
            tidligereKonfig.forEach { (key, value) ->
                if (value == null) System.clearProperty(key) else System.setProperty(key, value)
            }
        }
    }
}
