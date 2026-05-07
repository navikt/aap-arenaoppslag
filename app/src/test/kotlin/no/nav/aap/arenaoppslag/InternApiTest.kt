package no.nav.aap.arenaoppslag

import no.nav.aap.arenaoppslag.client.ArenaOppslagGateway.Companion.withTestServer
import no.nav.aap.arenaoppslag.database.H2TestBase
import no.nav.aap.arenaoppslag.kontrakt.intern.InternVedtakRequest
import no.nav.aap.arenaoppslag.kontrakt.intern.PerioderMed11_17Response
import no.nav.aap.arenaoppslag.kontrakt.intern.PerioderResponse
import no.nav.aap.arenaoppslag.kontrakt.intern.SakStatus
import no.nav.aap.arenaoppslag.kontrakt.intern.SakerRequest
import no.nav.aap.arenaoppslag.kontrakt.modeller.Maksimum
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class InternApiTest : H2TestBase("flyway/minimumtest", "flyway/eksisterer") {
    companion object {
        const val ukjentPerson = "007"
        const val kjentPerson = "12312312312"
    }

    @Test
    fun `Henter ut perioder for fellesordningen`() {
        withTestServer(h2) { gateway ->
            val request = InternVedtakRequest(
                personidentifikator = kjentPerson,
                fraOgMedDato = LocalDate.of(2022, 10, 1),
                tilOgMedDato = LocalDate.of(2023, 12, 31)
            )
            val allePerioder: PerioderResponse = gateway.hentPerioder(request)

            assertEquals(1, allePerioder.perioder.size)
        }
    }

    @Test
    fun `Henter ut perioder 11-17 for fellesordningen`() {
        withTestServer(h2) { gateway ->
            val request = InternVedtakRequest(
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
        withTestServer(h2) { gateway ->
            val request = InternVedtakRequest(
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
        withTestServer(h2) { gateway ->
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
        withTestServer(h2) { gateway ->
            val sakerForUkjentPerson: List<SakStatus> = gateway.hentSakerByFnr(
                SakerRequest(
                    personidentifikatorer = listOf(ukjentPerson)
                )
            )
            assertThat(sakerForUkjentPerson).isEmpty()
        }
    }


}
