package arenaoppslag.client

import arenaoppslag.util.AzureTokenGen
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.aap.arenaoppslag.kontrakt.ekstern.EksternVedtakRequest
import no.nav.aap.arenaoppslag.kontrakt.intern.KanBehandleSoknadIKelvin
import no.nav.aap.arenaoppslag.kontrakt.intern.PerioderMed11_17Response
import no.nav.aap.arenaoppslag.kontrakt.intern.PersonEksistererIAAPArena
import no.nav.aap.arenaoppslag.kontrakt.intern.PersonHarSignifikantAAPArenaHistorikk
import no.nav.aap.arenaoppslag.kontrakt.intern.SakStatus
import no.nav.aap.arenaoppslag.kontrakt.intern.SakerRequest
import no.nav.aap.arenaoppslag.kontrakt.intern.VedtakResponse
import no.nav.aap.arenaoppslag.kontrakt.modeller.Maksimum
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(ArenaOppslagGateway::class.java)

private val objectMapper =
    jacksonObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).registerModule(JavaTimeModule())


class ArenaOppslagGateway(private val tokenProvider: AzureTokenGen, private val httpClient: HttpClient) {
    suspend fun hentPerioder(
        vedtakRequest: EksternVedtakRequest
    ): VedtakResponse = gjørArenaOppslag<VedtakResponse, EksternVedtakRequest>(
        "/intern/perioder", vedtakRequest
    ).getOrThrow()

    suspend fun hentPerioderInkludert11_17(
        req: EksternVedtakRequest,
    ): PerioderMed11_17Response = gjørArenaOppslag<PerioderMed11_17Response, EksternVedtakRequest>(
        "/intern/perioder/11-17", req
    ).getOrThrow()

    suspend fun hentPersonEksistererIAapContext(
        req: SakerRequest,
    ): PersonEksistererIAAPArena =
        gjørArenaOppslag<PersonEksistererIAAPArena, SakerRequest>(
            "/intern/person/aap/eksisterer", req
        ).getOrThrow()

    suspend fun personHarSignifikantAAPArenaHistorikk(
        req: KanBehandleSoknadIKelvin
    ): PersonHarSignifikantAAPArenaHistorikk =
        gjørArenaOppslag<PersonHarSignifikantAAPArenaHistorikk, KanBehandleSoknadIKelvin>(
            "/intern/person/aap/signifikant-historikk", req
        ).getOrThrow()

    suspend fun hentSakerByFnr(
        req: SakerRequest
    ): List<SakStatus> =
        gjørArenaOppslag<List<SakStatus>, SakerRequest>(
            "/intern/saker", req
        ).getOrThrow()

    suspend fun hentMaksimum(
        req: EksternVedtakRequest
    ): Maksimum = gjørArenaOppslag<Maksimum, EksternVedtakRequest>(
        "/intern/maksimum", req
    ).getOrThrow()

    private suspend inline fun <reified T, reified V> gjørArenaOppslag(
        endepunkt: String, req: V
    ): Result<T> {
        // Vi starter en kjede av kall og prosessering, hvor hvert steg kan feile.
        var fikkToken = false
        var fikkArenaData = false

        val parsedResult = runCatching {
            val token = tokenProvider.generate().also {
                fikkToken = true
            }

            val arenaResponse = httpClient.post(endepunkt) {
                accept(ContentType.Application.Json)
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(req)
            }.also {
                if (it.status.isSuccess()) {
                    fikkArenaData = true
                }
            }

            objectMapper.readValue<T>(arenaResponse.bodyAsText())
        }.onFailure { e ->
            when {
                !fikkToken -> log.error("Fetch av token for Arena-oppslag feilet",e)
                !fikkArenaData -> log.error("Fetch av Arena-data feilet for '$endepunkt'", e)
                else -> {
                    log.error("Parsefeil for '$endepunkt'", e)
                }
            }
        }
        return parsedResult
    }

}
