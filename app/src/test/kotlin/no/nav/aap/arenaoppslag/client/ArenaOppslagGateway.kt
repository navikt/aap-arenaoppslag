package no.nav.aap.arenaoppslag.client

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.aap.arenaoppslag.TestConfig
import no.nav.aap.arenaoppslag.TestConfig.jsonHttpClient
import no.nav.aap.arenaoppslag.kontrakt.apiv1.ArenaVedtakMedDetaljer
import no.nav.aap.arenaoppslag.kontrakt.apiv1.VedtakForPersonRequest
import no.nav.aap.arenaoppslag.kontrakt.apiv1.MaksdatoRequest
import no.nav.aap.arenaoppslag.kontrakt.apiv1.MaksdatoResponse
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SisteUtbetalingerRequest
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SisteUtbetalingerResponse
import no.nav.aap.arenaoppslag.kontrakt.intern.InternVedtakRequest
import no.nav.aap.arenaoppslag.kontrakt.intern.PerioderMed11_17Response
import no.nav.aap.arenaoppslag.kontrakt.intern.PerioderResponse
import no.nav.aap.arenaoppslag.kontrakt.intern.PersonEksistererIAAPArena
import no.nav.aap.arenaoppslag.kontrakt.intern.SakStatus
import no.nav.aap.arenaoppslag.kontrakt.intern.SakerRequest
import no.nav.aap.arenaoppslag.kontrakt.intern.SignifikanteSakerRequest
import no.nav.aap.arenaoppslag.kontrakt.intern.SignifikanteSakerResponse
import no.nav.aap.arenaoppslag.kontrakt.modeller.Maksimum
import no.nav.aap.arenaoppslag.server
import no.nav.aap.arenaoppslag.util.AzureTokenGen
import no.nav.aap.arenaoppslag.util.FakePdlGateway
import no.nav.aap.arenaoppslag.util.Fakes
import org.slf4j.LoggerFactory
import javax.sql.DataSource

private val log = LoggerFactory.getLogger(ArenaOppslagGateway::class.java)

private val objectMapper =
    jacksonObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).registerModule(JavaTimeModule())


class ArenaOppslagGateway(private val tokenProvider: AzureTokenGen, private val httpClient: HttpClient) {
    suspend fun hentPerioder(
        vedtakRequest: InternVedtakRequest

    ): PerioderResponse = gjørArenaOppslag<PerioderResponse, InternVedtakRequest
            >(
        "/intern/perioder", vedtakRequest
    ).getOrThrow()

    suspend fun hentPerioderInkludert11_17(
        req: InternVedtakRequest,
    ): PerioderMed11_17Response = gjørArenaOppslag<PerioderMed11_17Response, InternVedtakRequest
            >(
        "/intern/perioder/11-17", req
    ).getOrThrow()

    suspend fun hentPersonEksistererIAapContext(
        req: SakerRequest,
    ): PersonEksistererIAAPArena =
        gjørArenaOppslag<PersonEksistererIAAPArena, SakerRequest>(
            "/api/v1/person/eksisterer", req
        ).getOrThrow()

    suspend fun personHarSignifikantAAPArenaHistorikk(
        req: SignifikanteSakerRequest
    ): SignifikanteSakerResponse =
        gjørArenaOppslag<SignifikanteSakerResponse, SignifikanteSakerRequest>(
            "/api/v1/person/signifikant-historikk", req
        ).getOrThrow()

    suspend fun hentMaksdatoBySakIdListe(
        req: MaksdatoRequest
    ): MaksdatoResponse =
        gjørArenaOppslag<MaksdatoResponse, MaksdatoRequest>(
            "/api/v1/maksdato", req
        ).getOrThrow()

    suspend fun hentSisteUtbetalingISaker(
        req: SisteUtbetalingerRequest
    ): SisteUtbetalingerResponse =
        gjørArenaOppslag<SisteUtbetalingerResponse, SisteUtbetalingerRequest>(
            "/api/v1/utbetalinger/siste", req
        ).getOrThrow()

    suspend fun hentVedtakDetaljerForPerson(
        req: VedtakForPersonRequest
    ): List<ArenaVedtakMedDetaljer> =
        gjørArenaOppslag<List<ArenaVedtakMedDetaljer>, VedtakForPersonRequest>(
            "/api/v1/person/vedtak/detaljert", req
        ).getOrThrow()

    suspend fun hentSakerByFnr(
        req: SakerRequest
    ): List<SakStatus> =
        gjørArenaOppslag<List<SakStatus>, SakerRequest>(
            "/intern/saker", req
        ).getOrThrow()

    suspend fun hentMaksimum(
        req: InternVedtakRequest

    ): Maksimum = gjørArenaOppslag<Maksimum, InternVedtakRequest
            >(
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
                !fikkToken -> log.error("Fetch av token for Arena-oppslag feilet", e)
                !fikkArenaData -> log.error("Fetch av Arena-data feilet for '$endepunkt'", e)
                else -> {
                    log.error("Parsefeil for '$endepunkt'", e)
                }
            }
        }
        return parsedResult
    }

    companion object {
        fun withTestServer(dataSource: DataSource, testBody: suspend (ArenaOppslagGateway) -> Unit) {
            val config = TestConfig.default(Fakes())
            val tokenProvider = AzureTokenGen(config.azure.issuer, config.azure.clientId)
            testApplication {
                application { server(config, dataSource, FakePdlGateway()) }
                val gateway = ArenaOppslagGateway(tokenProvider, jsonHttpClient)

                testBody(gateway)
            }
        }
    }

}
