package no.nav.aap.arenaoppslag.tilgang

import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.*
import io.ktor.http.*
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import no.nav.aap.arenaoppslag.DefaultJsonMapper
import no.nav.aap.arenaoppslag.modeller.PersonIdentifikator
import no.nav.aap.arenaoppslag.plugins.MdcKeys
import no.nav.aap.arenaoppslag.tilgang.TilgangGatewayImpl.TilgangException
import no.nav.aap.arenaoppslag.util.AzureTokenGen
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.TokenProvider
import no.nav.aap.tilgang.ArenasakTilgangRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.slf4j.MDC
import java.io.IOException

class TilgangGatewayImplTest {
    private val scope = "api://test.aap.tilgang/.default"
    private val konfig = mapOf(
        "INTEGRASJON_TILGANG_URL" to "http://tilgang/",
        "INTEGRASJON_TILGANG_SCOPE" to scope,
    )
    private var tidligereKonfig = emptyMap<String, String?>()
    private val brukerToken = OidcToken(AzureTokenGen("issuer", "arenaoppslag").generate())
    private val oboToken = OidcToken(AzureTokenGen("issuer", "tilgang").generate())
    private val tokenProvider = mockk<TokenProvider>()
    private val testPerson = PersonIdentifikator("12312312312")
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
    private val mapper = DefaultJsonMapper.objectMapper()

    @BeforeEach
    fun settOppKonfig() {
        tidligereKonfig = konfig.mapValues { (key, _) -> System.getProperty(key) }
        konfig.forEach { (key, value) -> System.setProperty(key, value) }
        every { tokenProvider.getToken(scope, brukerToken) } returns oboToken
    }

    @AfterEach
    fun gjenopprettKonfig() {
        tidligereKonfig.forEach { (key, value) ->
            if (value == null) System.clearProperty(key) else System.setProperty(key, value)
        }
    }

    private suspend fun medGateway(
        handler: MockRequestHandler,
        test: suspend (TilgangGatewayImpl, MockEngine) -> Unit,
    ) {
        val engine = MockEngine(handler)
        try {
            TilgangGatewayImpl(TilgangHttpClient.lagHttpClient(engine), tokenProvider).use { gateway ->
                test(gateway, engine)
            }
        } finally {
            engine.close()
        }
    }

    @Test
    fun `opprettelse og lukking krever ikke tilgangskonfig`(): Unit = runBlocking {
        konfig.keys.forEach(System::clearProperty)
        medGateway({ error("Skal ikke kalle tilgang") }) { _, engine ->
            assertThat(engine.requestHistory).isEmpty()
            verify(exactly = 0) { tokenProvider.getToken(any(), any()) }
        }
    }

    @Test
    fun `leser tilgangskonfig ved første tilgangskall`(): Unit = runBlocking {
        konfig.keys.forEach(System::clearProperty)
        medGateway({
            respond("""{"tilgang":true}""", HttpStatusCode.OK, jsonHeaders)
        }) { gateway, engine ->
            konfig.forEach { (key, value) -> System.setProperty(key, value) }
            assertThat(gateway.harTilgangTilPerson(testPerson, brukerToken).tilgang).isTrue()
            assertThat(engine.requestHistory).hasSize(1)
            verify(exactly = 1) { tokenProvider.getToken(scope, brukerToken) }
        }
    }

    @Test
    fun `bruker OBO-token fra provider med riktig payload og kall-id`(): Unit = runBlocking {
        val tidligereCallId = MDC.get(MdcKeys.CallId)
        MDC.put(MdcKeys.CallId, "test-call-id")
        try {
            medGateway({ request ->
                assertThat(request.method).isEqualTo(HttpMethod.Post)
                assertThat(request.body.contentType).isEqualTo(ContentType.Application.Json)
                assertThat(request.url.toString()).isEqualTo("http://tilgang/tilgang/arenasaker")
                assertThat(request.headers[HttpHeaders.Authorization]).isEqualTo("Bearer ${oboToken.token()}")
                assertThat(request.headers["Nav-CallId"]).isEqualTo("test-call-id")
                assertThat(mapper.readTree(request.body.toByteArray()))
                    .isEqualTo(mapper.valueToTree(ArenasakTilgangRequest(personIdent = testPerson.id)))
                respond("""{"tilgang":true,"tilgangIKontekst":null}""", HttpStatusCode.OK, jsonHeaders)
            }) { gateway, engine ->
                assertThat(gateway.harTilgangTilPerson(testPerson, brukerToken).tilgang).isTrue()
                assertThat(engine.requestHistory).hasSize(1)
                verify(exactly = 1) { tokenProvider.getToken(scope, brukerToken) }
            }
        } finally {
            if (tidligereCallId == null) MDC.remove(MdcKeys.CallId) else MDC.put(MdcKeys.CallId, tidligereCallId)
        }
    }

    @Test
    fun `200 OK med tilgang false gir avslag uten detaljer`(): Unit = runBlocking {
        medGateway({ respond("""{"tilgang":false}""", HttpStatusCode.OK, jsonHeaders) }) { gateway, engine ->
            val resultat = gateway.harTilgangTilPerson(testPerson, brukerToken)
            assertThat(resultat.tilgang).isFalse()
            assertThat(resultat.tilgangIKontekst).isNull()
            assertThat(engine.requestHistory).hasSize(1)
        }
    }

    @Test
    fun `ugyldig tilgangssvar er integrasjonsfeil`(): Unit = runBlocking {
        for (body in listOf("", "ikke JSON", "[]", "null", "{}", """{"tilgang":null}""")) {
            medGateway({ respond(body, HttpStatusCode.OK, jsonHeaders) }) { gateway, engine ->
                assertThrows<TilgangException> { gateway.harTilgangTilPerson(testPerson, brukerToken) }
                assertThat(engine.requestHistory).hasSize(1)
            }
        }
    }

    @Test
    fun `HTTP-feil og tomt 204-svar fra tilgang gir feil uten nye forsøk`(): Unit = runBlocking {
        for (status in listOf(
            HttpStatusCode.BadRequest,
            HttpStatusCode.Unauthorized,
            HttpStatusCode.Forbidden,
            HttpStatusCode.InternalServerError,
            HttpStatusCode.ServiceUnavailable,
            HttpStatusCode.NoContent,
        )) {
            medGateway({ respond("", status, jsonHeaders) }) { gateway, engine ->
                assertThrows<TilgangException> { gateway.harTilgangTilPerson(testPerson, brukerToken) }
                assertThat(engine.requestHistory).hasSize(1)
            }
        }
    }

    @Test
    fun `nettverksfeil og timeout mot tilgang prøves ikke på nytt`(): Unit = runBlocking {
        for (timeout in listOf(false, true)) {
            var tilgangskall = 0
            medGateway({ request ->
                tilgangskall++
                if (timeout) throw HttpRequestTimeoutException(request.url.toString(), 5_000)
                throw IOException("Connection reset")
            }) { gateway, _ ->
                assertThrows<TilgangException> { gateway.harTilgangTilPerson(testPerson, brukerToken) }
                assertThat(tilgangskall).isEqualTo(1)
            }
        }
    }

    @Test
    fun `providerfeil og manglende token stopper før tilgangskallet`(): Unit = runBlocking {
        for (feiler in listOf(false, true)) {
            every { tokenProvider.getToken(scope, brukerToken) } answers {
                if (feiler) throw IOException("Texas utilgjengelig")
                null
            }
            medGateway({ error("Skal ikke kalle tilgang") }) { gateway, engine ->
                val feil = assertThrows<TilgangException> { gateway.harTilgangTilPerson(testPerson, brukerToken) }
                assertThat(feil.message).contains("OBO-token")
                assertThat(engine.requestHistory).isEmpty()
            }
        }
    }

    @Test
    fun `kansellering fra provider og tilgang propageres uten nye forsøk`(): Unit = runBlocking {
        val kansellering = CancellationException("Avbrutt")
        every { tokenProvider.getToken(scope, brukerToken) } throws kansellering
        medGateway({ error("Skal ikke kalle tilgang") }) { gateway, engine ->
            assertThrows<CancellationException> { gateway.harTilgangTilPerson(testPerson, brukerToken) }
            assertThat(engine.requestHistory).isEmpty()
        }
        every { tokenProvider.getToken(scope, brukerToken) } returns oboToken
        var antallKall = 0
        medGateway({
            antallKall++
            throw kansellering
        }) { gateway, _ ->
            assertThrows<CancellationException> { gateway.harTilgangTilPerson(testPerson, brukerToken) }
            assertThat(antallKall).isEqualTo(1)
        }
    }
}
