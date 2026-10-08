package no.nav.aap.arenaoppslag.tilgangsmaskin

import com.fasterxml.jackson.databind.DeserializationFeature
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withTimeoutOrNull
import no.nav.aap.arenaoppslag.DefaultJsonMapper
import no.nav.aap.arenaoppslag.plugins.MdcKeys
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.TokenProvider
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureOBOTokenProvider
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.io.IOException
import java.net.http.HttpTimeoutException
import java.util.*
import kotlin.coroutines.cancellation.CancellationException

/**
 * Se Confluence for dokumentasjon.
 * https://confluence.adeo.no/spaces/TM/pages/628888614/Intro+til+Tilgangsmaskinen
 */
class TilgangmaskinGatewayImpl(
    private val httpClient: HttpClient = TilgangMaskinHttpClient.lagHttpClient(),
    private val tokenProvider: TokenProvider = AzureOBOTokenProvider,
) : TilgangsmaskinGateway {
    private val baseUrl = requiredConfigForKey("integrasjon.tilgangsmaskin.url").trimEnd('/')
    private val scope = requiredConfigForKey("integrasjon.tilgangsmaskin.scope")

    override suspend fun harTilgangTilPerson(
        personIdentifikator: String,
        token: OidcToken
    ): TilgangsmaskinKomplettResponse {
        val oboToken = hentOboToken(token)
        // Propager korrelasjons-id-en fra inngående request (som CallId-pluginen legger i MDC) slik at
        // kallet kan spores på tvers av tjenester. Tilgangsmaskinen leser X-Correlation-ID. Faller
        // tilbake på en ny UUID om MDC mangler verdien.
        val correlationId = MDC.get(MdcKeys.CallId) ?: UUID.randomUUID().toString()

        return try {
            httpClient.post("$baseUrl/api/v1/komplett") {
                accept(ContentType.Application.Json)
                bearerAuth(oboToken.token())
                header(HttpHeaders.XCorrelationId, correlationId)
                header(NAV_CONSUMER_ID_HEADER, consumerId)
                contentType(ContentType.Text.Plain)
                setBody(personIdentifikator)
            }
            TilgangsmaskinKomplettResponse(harTilgang = true)
        } catch (e: ResponseException) {
            // Kun 403 betyr avslag fra Tilgangsmaskinen; 401 o.l. er feil i integrasjonen og skal ikke tolkes som avslag
            if (e.response.status == HttpStatusCode.Forbidden) {
                // Selve avvisningen (nav-ident, årsak, maskert fnr) logges i TilgangkontrollService,
                // der vi har nav-ident og fødselsnummer tilgjengelig.
                val avvistResponse = parseAvvistResponse(e.response)
                TilgangsmaskinKomplettResponse(harTilgang = false, avvistResponse = avvistResponse)
            } else {
                throw TilgangsmaskinException("Kall mot Tilgangsmaskinen feilet med status ${e.response.status}", e)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw TilgangsmaskinException("Kall mot Tilgangsmaskinen feilet", e)
        }
    }

    private suspend fun parseAvvistResponse(response: HttpResponse): TilgangsmaskinAvvistResponse? = try {
        DefaultJsonMapper.objectMapper()
            .readerFor(TilgangsmaskinAvvistResponse::class.java)
            .with(
                DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES,
                DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES,
            )
            .readValue<TilgangsmaskinAvvistResponse>(response.bodyAsText())
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        currentCoroutineContext().ensureActive()
        logger.warn("Klarte ikke å parse avvistresponse fra Tilgangsmaskinen ({})", e.javaClass.simpleName)
        null
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun hentOboToken(token: OidcToken): OidcToken = try {
        withTimeoutOrNull(OBO_TOKEN_TIMEOUT_MS) {
            try {
                runInterruptible(Dispatchers.IO) {
                    checkNotNull(tokenProvider.getToken(scope, token)) { "Tokenprovideren returnerte ikke noe token" }
                }
            } catch (e: Exception) {
                currentCoroutineContext().ensureActive()
                throw e
            }
        } ?: throw HttpTimeoutException("Henting av OBO-token tok mer enn $OBO_TOKEN_TIMEOUT_MS ms")
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        throw TilgangsmaskinException("Henting av OBO-token for Tilgangsmaskinen feilet", e)
    }

    override fun close() {
        httpClient.close()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TilgangmaskinGatewayImpl::class.java)

        private const val OBO_TOKEN_TIMEOUT_MS = 2_000L
        private const val NAV_CONSUMER_ID_HEADER = "Nav-Consumer-Id"
        private val consumerId = System.getenv("NAIS_APP_NAME") ?: "arenaoppslag"
    }

    class TilgangsmaskinException(melding: String, cause: Throwable) : RuntimeException(melding, cause)
}
