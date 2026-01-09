package arenaoppslag

import arenaoppslag.client.ArenaOppslagGateway
import arenaoppslag.database.H2TestBase
import arenaoppslag.util.AzureTokenGen
import arenaoppslag.util.Fakes
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import no.nav.aap.arenaoppslag.kontrakt.ekstern.EksternVedtakRequest
import no.nav.aap.arenaoppslag.kontrakt.intern.KanBehandleSoknadIKelvin
import no.nav.aap.arenaoppslag.kontrakt.intern.PerioderMed11_17Response
import no.nav.aap.arenaoppslag.kontrakt.intern.PersonEksistererIAAPArena
import no.nav.aap.arenaoppslag.kontrakt.intern.PersonHarSignifikantAAPArenaHistorikk
import no.nav.aap.arenaoppslag.kontrakt.intern.SakStatus
import no.nav.aap.arenaoppslag.kontrakt.intern.SakerRequest
import no.nav.aap.arenaoppslag.kontrakt.intern.VedtakResponse
import no.nav.aap.arenaoppslag.kontrakt.modeller.Maksimum
import no.nav.aap.komponenter.json.DefaultJsonMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ApiTest : H2TestBase("flyway/minimumtest", "flyway/eksisterer") {
    companion object {
        const val ukjentPerson = "007"
        const val kjentPerson = "1"
    }

    @Test
    fun `Henter ut perioder for fellesordningen`() {
        withTestServer { gateway ->
            val request = EksternVedtakRequest(
                personidentifikator = kjentPerson,
                fraOgMedDato = LocalDate.of(2022, 10, 1),
                tilOgMedDato = LocalDate.of(2023, 12, 31)
            )
            val alleVedtak: VedtakResponse = gateway.hentPerioder(request)

            assertEquals(1, alleVedtak.perioder.size)
        }
    }

    @Test
    fun `Henter ut perioder 11-17 for fellesordningen`() {
        withTestServer { gateway ->
            val request = EksternVedtakRequest(
                personidentifikator = kjentPerson,
                fraOgMedDato = LocalDate.of(2022, 1, 1),
                tilOgMedDato = LocalDate.of(2023, 12, 31)
            )
            val alleVedtak: PerioderMed11_17Response = gateway.hentPerioderInkludert11_17(request)

            assertEquals(1, alleVedtak.perioder.size)
        }
    }

    @Test
    fun `Henter ut maksimumsvedtak for fellesordningen`() {
        withTestServer { gateway ->
            val request = EksternVedtakRequest(
                personidentifikator = kjentPerson,
                fraOgMedDato = LocalDate.of(2022, 10, 1),
                tilOgMedDato = LocalDate.of(2023, 12, 31)
            )
            val alleVedtak: Maksimum = gateway.hentMaksimum(request)

            assertEquals(1, alleVedtak.vedtak.size)
        }
    }

    @Test
    fun `Henter ut saker by fnr, kjent person`() {
        withTestServer { gateway ->
            val sakerForKjentPerson: List<SakStatus> = gateway.hentSakerByFnr(
                SakerRequest(
                    personidentifikatorer = listOf(kjentPerson)
                )
            )
            assertThat(sakerForKjentPerson).isNotEmpty()
        }
    }

    @Test
    fun `Henter ut saker by fnr, ukjent person`() {
        withTestServer { gateway ->
            val sakerForUkjentPerson: List<SakStatus> = gateway.hentSakerByFnr(
                SakerRequest(
                    personidentifikatorer = listOf(ukjentPerson)
                )
            )
            assertThat(sakerForUkjentPerson).isEmpty()
        }
    }


    @Test
    fun `Person eksisterer i AAP-Arena, ja`() {
        withTestServer { gateway ->
            val kjentPerson: PersonEksistererIAAPArena = gateway.hentPersonEksistererIAapContext(
                SakerRequest(
                    personidentifikatorer = listOf(kjentPerson)
                )
            )
            assertThat(kjentPerson.eksisterer).isTrue
        }
    }

    @Test
    fun `Person eksisterer i AAP-Arena, nei`() {
        withTestServer { gateway ->
            val ukjentPerson: PersonEksistererIAAPArena = gateway.hentPersonEksistererIAapContext(
                SakerRequest(
                    personidentifikatorer = listOf(ukjentPerson)
                )
            )
            assertThat(ukjentPerson.eksisterer).isFalse
        }
    }


    @Test
    fun `Person har relevant historikk i AAP-Arena`() {
        withTestServer { gateway ->
            val kjentPerson: PersonHarSignifikantAAPArenaHistorikk = gateway.personHarSignifikantAAPArenaHistorikk(
                KanBehandleSoknadIKelvin(
                    personidentifikatorer = listOf(kjentPerson),
                    virkningstidspunkt = LocalDate.of(2022, 10, 1),
                )
            )

            assertThat(kjentPerson.harSignifikantHistorikk).isFalse
            // legg til flere tester når vi har en godkjent spørring
        }
    }


    private fun withTestServer(testBody: suspend (ArenaOppslagGateway) -> Unit) {
        val config = TestConfig.default(Fakes())
        val tokenProvider = AzureTokenGen(config.azure.issuer, config.azure.clientId)
        testApplication {
            application { server(config, h2) }
            val gateway = ArenaOppslagGateway(tokenProvider, jsonHttpClient)

            testBody(gateway)
        }
    }

    private val ApplicationTestBuilder.jsonHttpClient: HttpClient
        get() = createClient {
            expectSuccess = true // Kaster exception for 4xx og 5xx svar, altså feiler testen

            install(ContentNegotiation) {
                register(
                    ContentType.Application.Json,
                    JacksonConverter(DefaultJsonMapper.objectMapper())
                )
            }
        }

}
