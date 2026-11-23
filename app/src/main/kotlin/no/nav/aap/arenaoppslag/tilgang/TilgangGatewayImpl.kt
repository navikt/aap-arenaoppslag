package no.nav.aap.arenaoppslag.tilgang

import com.fasterxml.jackson.databind.DeserializationFeature
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import no.nav.aap.arenaoppslag.DefaultJsonMapper
import no.nav.aap.arenaoppslag.modeller.PersonIdentifikator
import no.nav.aap.arenaoppslag.plugins.MdcKeys
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.TokenProvider
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureOBOTokenProvider
import no.nav.aap.tilgang.ArenasakTilgangRequest
import org.slf4j.MDC
import java.util.*
import kotlin.coroutines.cancellation.CancellationException
import no.nav.aap.tilgang.TilgangResponse as TilgangApiResponse

class TilgangGatewayImpl(
    private val httpClient: HttpClient = TilgangHttpClient.lagHttpClient(),
    private val tokenProvider: TokenProvider = AzureOBOTokenProvider,
) : TilgangGateway {
    private val baseUrl by lazy { requiredConfigForKey("INTEGRASJON_TILGANG_URL").trimEnd('/') }
    private val scope by lazy { requiredConfigForKey("INTEGRASJON_TILGANG_SCOPE") }

    override suspend fun harTilgangTilPerson(
        personIdentifikator: PersonIdentifikator,
        token: OidcToken
    ): TilgangApiResponse {
        val oboToken = hentOboTokenFraTexas(token)
        val correlationId = MDC.get(MdcKeys.CallId) ?: UUID.randomUUID().toString()

        return try {
            val response = httpClient.post("$baseUrl/tilgang/arenasaker") {
                accept(ContentType.Application.Json)
                retry { noRetry() }
                bearerAuth(oboToken)
                header("Nav-CallId", correlationId)
                contentType(ContentType.Application.Json)
                setBody(ArenasakTilgangRequest(personIdent = personIdentifikator.id))
            }
            check(response.status == HttpStatusCode.OK) {
                // Kast exception som fanges opp til slutt i metoden
                "Forventet 200 fra aap-tilgang, fikk ${response.status}"
            }
            val tilgang = DefaultJsonMapper.objectMapper()
                .readerFor(TilgangApiResponse::class.java)
                .with(
                    DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES,
                    DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES,
                )
                .readValue<TilgangApiResponse>(response.bodyAsText())
            tilgang
        } catch (e: ResponseException) {
            throw TilgangException("Kall mot aap-tilgang feilet med status ${e.response.status}", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw TilgangException("Kall mot aap-tilgang feilet", e)
        }
    }

    private suspend fun hentOboTokenFraTexas(token: OidcToken): String = try {
        runInterruptible(Dispatchers.IO) {
            requireNotNull(tokenProvider.getToken(scope, token)) { "Mottok ikke OBO-token fra Texas" }
                .token()
                .also { require(it.isNotBlank()) { "Mottok tomt OBO-token fra Texas" } }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        throw TilgangException("Henting av OBO-token for aap-tilgang feilet", e)
    }

    override fun close() {
        httpClient.close()
    }

    class TilgangException(melding: String, cause: Throwable) : RuntimeException(melding, cause)

}
