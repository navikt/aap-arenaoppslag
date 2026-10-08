package no.nav.aap.arenaoppslag.tilgangsmaskin

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeoutCapability
import java.io.IOException
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import no.nav.aap.arenaoppslag.util.AzureTokenGen
import no.nav.aap.arenaoppslag.tilgangsmaskin.TilgangmaskinGatewayImpl.TilgangsmaskinException
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.TokenProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TilgangmaskinGatewayImplTest {
    companion object {
        @JvmStatic
        @BeforeAll
        fun settOppKonfig() {
            System.setProperty("integrasjon.tilgangsmaskin.url", "http://tilgangsmaskin")
            System.setProperty("integrasjon.tilgangsmaskin.scope", "tilgangsmaskin-scope")
        }
    }

    private val brukerToken = OidcToken(AzureTokenGen("issuer", "arenaoppslag").generate())
    private val oboToken = OidcToken(AzureTokenGen("issuer", "tilgangsmaskin").generate())

    private val fakeTokenProvider = object : TokenProvider {
        override fun getToken(scope: String?, currentToken: OidcToken?): OidcToken {
            assertThat(scope).isEqualTo("tilgangsmaskin-scope")
            assertThat(currentToken?.token()).isEqualTo(brukerToken.token())
            return oboToken
        }
    }

    private fun lagGateway(vararg statuser: HttpStatusCode): Pair<TilgangmaskinGatewayImpl, MockEngine> {
        var kall = 0
        val engine = MockEngine { request ->
            assertThat(request.url.toString()).isEqualTo("http://tilgangsmaskin/api/v1/komplett")
            assertThat(request.headers[HttpHeaders.Authorization]).isEqualTo("Bearer ${oboToken.token()}")
            assertThat(String(request.body.toByteArray())).isEqualTo("12312312312")
            respond("", statuser[minOf(kall++, statuser.lastIndex)])
        }
        val gateway = TilgangmaskinGatewayImpl(TilgangMaskinHttpClient.lagHttpClient(engine), fakeTokenProvider)
        return gateway to engine
    }

    @Test
    fun `gir tilgang ved 204`(): Unit = runBlocking {
        val (gateway, engine) = lagGateway(HttpStatusCode.NoContent)

        assertThat(gateway.harTilgangTilPerson("12312312312", brukerToken).harTilgang).isTrue()
        assertThat(engine.requestHistory).hasSize(1)
    }

    @Test
    fun `avslår ved 403 uten nye forsøk`(): Unit = runBlocking {
        val (gateway, engine) = lagGateway(HttpStatusCode.Forbidden)

        assertThat(gateway.harTilgangTilPerson("12312312312", brukerToken).harTilgang).isFalse()
        assertThat(engine.requestHistory).hasSize(1)
    }

    @Test
    fun `tolker avslagskroppen fra 403 og gjør den tilgjengelig for kalleren`(): Unit = runBlocking {
        val body = """
            {
              "type": "AVVIST_GEOGRAFISK",
              "title": "AVVIST_GEOGRAFISK",
              "status": 403,
              "navIdent": "Z123456",
              "begrunnelse": "Mangler geografisk tilgang",
              "kanOverstyres": true,
              "ukjentFelt": "skal ignoreres"
            }
        """.trimIndent()
        val engine = MockEngine {
            respond(
                body,
                HttpStatusCode.Forbidden,
                headersOf(HttpHeaders.ContentType, "application/problem+json"),
            )
        }
        val gateway = TilgangmaskinGatewayImpl(TilgangMaskinHttpClient.lagHttpClient(engine), fakeTokenProvider)

        val resultat = gateway.harTilgangTilPerson("12312312312", brukerToken)

        assertThat(resultat.harTilgang).isFalse()
        assertThat(resultat.avvistResponse).isNotNull()
        assertThat(resultat.avvistResponse?.title).isEqualTo("AVVIST_GEOGRAFISK")
        assertThat(resultat.avvistResponse?.begrunnelse).isEqualTo("Mangler geografisk tilgang")
        assertThat(resultat.avvistResponse?.kanOverstyres).isTrue()
    }

    @Test
    fun `sender med X-Correlation-ID og Nav-Consumer-Id`(): Unit = runBlocking {
        var correlationId: String? = null
        var consumerId: String? = null
        val engine = MockEngine { request ->
            correlationId = request.headers[HttpHeaders.XCorrelationId]
            consumerId = request.headers["Nav-Consumer-Id"]
            respond("", HttpStatusCode.NoContent)
        }
        val gateway = TilgangmaskinGatewayImpl(TilgangMaskinHttpClient.lagHttpClient(engine), fakeTokenProvider)

        gateway.harTilgangTilPerson("12312312312", brukerToken)

        assertThat(correlationId).isNotBlank()
        assertThat(consumerId).isEqualTo("arenaoppslag")
    }

    @Test
    fun `kaster uten nye forsøk ved 401 i stedet for å tolke det som avslag`() {
        val (gateway, engine) = lagGateway(HttpStatusCode.Unauthorized)

        val feil = assertThrows<TilgangsmaskinException> {
            runBlocking { gateway.harTilgangTilPerson("12312312312", brukerToken) }
        }
        assertThat(feil.cause).isInstanceOf(ClientRequestException::class.java)
        assertThat(engine.requestHistory).hasSize(1)
    }

    @Test
    fun `prøver ikke på nytt ved andre 4xx`() {
        val (gateway, engine) = lagGateway(HttpStatusCode.BadRequest, HttpStatusCode.NoContent)

        assertThrows<TilgangsmaskinException> {
            runBlocking { gateway.harTilgangTilPerson("12312312312", brukerToken) }
        }
        assertThat(engine.requestHistory).hasSize(1)
    }

    @Test
    fun `prøver på nytt ved nettverksfeil`(): Unit = runBlocking {
        var kall = 0
        val engine = MockEngine {
            if (kall++ == 0) throw IOException("Connection reset")
            respond("", HttpStatusCode.NoContent)
        }
        val gateway = TilgangmaskinGatewayImpl(TilgangMaskinHttpClient.lagHttpClient(engine), fakeTokenProvider)

        assertThat(gateway.harTilgangTilPerson("12312312312", brukerToken).harTilgang).isTrue()
        assertThat(kall).isEqualTo(2)
    }

    @Test
    fun `prøver på nytt ved timeout og øker timeouten for hvert forsøk`(): Unit = runBlocking {
        val timeouter = mutableListOf<Long?>()
        var kall = 0
        val engine = MockEngine { request ->
            timeouter += request.getCapabilityOrNull(HttpTimeoutCapability)?.requestTimeoutMillis
            if (kall++ < 2) throw HttpRequestTimeoutException(request.url.toString(), 1_000)
            respond("", HttpStatusCode.NoContent)
        }
        val gateway = TilgangmaskinGatewayImpl(TilgangMaskinHttpClient.lagHttpClient(engine), fakeTokenProvider)

        assertThat(gateway.harTilgangTilPerson("12312312312", brukerToken).harTilgang).isTrue()
        assertThat(kall).isEqualTo(3)
        assertThat(timeouter).containsExactly(1_000L, 2_000L, 4_000L)
    }

    @Test
    fun `gir opp etter maks antall forsøk ved vedvarende timeout`() {
        var kall = 0
        val engine = MockEngine { request ->
            kall++
            throw HttpRequestTimeoutException(request.url.toString(), 1_000)
        }
        val gateway = TilgangmaskinGatewayImpl(TilgangMaskinHttpClient.lagHttpClient(engine), fakeTokenProvider)

        assertThrows<TilgangsmaskinException> {
            runBlocking { gateway.harTilgangTilPerson("12312312312", brukerToken) }
        }
        assertThat(kall).isEqualTo(4) // 1 opprinnelig forsøk + 3 nye forsøk
    }

    @Test
    fun `kaster med kontekst når OBO-token ikke kan hentes`() {
        val feilendeTokenProvider = object : TokenProvider {
            override fun getToken(scope: String?, currentToken: OidcToken?): OidcToken = error("Texas er nede")
        }
        val engine = MockEngine { respond("", HttpStatusCode.NoContent) }
        val gateway = TilgangmaskinGatewayImpl(TilgangMaskinHttpClient.lagHttpClient(engine), feilendeTokenProvider)

        val feil = assertThrows<TilgangsmaskinException> {
            runBlocking { gateway.harTilgangTilPerson("12312312312", brukerToken) }
        }
        assertThat(feil.message).contains("OBO-token")
        assertThat(engine.requestHistory).isEmpty()
    }

    @Test
    fun `prøver på nytt ved serverfeil`(): Unit = runBlocking {
        val (gateway, engine) = lagGateway(
            HttpStatusCode.ServiceUnavailable,
            HttpStatusCode.InternalServerError,
            HttpStatusCode.NoContent,
        )

        assertThat(gateway.harTilgangTilPerson("12312312312", brukerToken).harTilgang).isTrue()
        assertThat(engine.requestHistory).hasSize(3)
    }
}
