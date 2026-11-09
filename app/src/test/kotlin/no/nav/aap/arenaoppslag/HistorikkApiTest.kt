package no.nav.aap.arenaoppslag

import no.nav.aap.arenaoppslag.client.ArenaOppslagGateway.Companion.withTestServer
import no.nav.aap.arenaoppslag.database.H2TestBase
import no.nav.aap.arenaoppslag.kontrakt.apiv1.HarHistorikkRequest
import no.nav.aap.arenaoppslag.kontrakt.apiv1.HarHistorikkResponse
import no.nav.aap.arenaoppslag.kontrakt.apiv1.HistorikkStatusDetaljResponse
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SignifikantHistorikkRequest
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SignifikantHistorikkResponse
import no.nav.aap.arenaoppslag.kontrakt.apiv1.TelleverkForPerson
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class HistorikkApiTest : H2TestBase("flyway/minimumtest", "flyway/eksisterer", "flyway/historikkapi") {
    companion object {
        const val ukjentPerson = "007"
        const val kjentPerson = "kun_nye"
    }

    private val testDato = LocalDate.parse("2025-12-15")

    @Test
    fun `Person har signifikant historikk for AAP i AAP-Arena`() {
        withTestServer(h2) { gateway ->
            val response: SignifikantHistorikkResponse = gateway.harSignifikantHistorikk(
                SignifikantHistorikkRequest(
                    kjentPerson,
                    testDato
                )
            )

            assertThat(response.harSignifikantHistorikk).isTrue
        }
    }

    @Test
    fun `Person har IKKE signifikant historikk i AAP-Arena`() {
        withTestServer(h2) { gateway ->
            val ukjentPerson: SignifikantHistorikkResponse = gateway.harSignifikantHistorikk(
                SignifikantHistorikkRequest(
                    ukjentPerson,
                    LocalDateTime.now().minusDays(1).toLocalDate()
                )
            )

            assertThat(ukjentPerson.harSignifikantHistorikk).isFalse
        }
    }

    @Test
    fun `Person eksisterer i AAP-Arena, ja`() {
        withTestServer(h2) { gateway ->
            val kjentPerson: HarHistorikkResponse = gateway.harHistorikk(HarHistorikkRequest(kjentPerson))
            assertThat(kjentPerson.harHistorikk).isTrue
        }
    }

    @Test
    fun `Person eksisterer i AAP-Arena, nei`() {
        withTestServer(h2) { gateway ->
            val ukjentPerson: HarHistorikkResponse = gateway.harHistorikk(HarHistorikkRequest(ukjentPerson))
            assertThat(ukjentPerson.harHistorikk).isFalse
        }
    }

    @Test
    fun `Henter telleverk for kjent person uten løpende vedtak`() {
        withTestServer(h2) { gateway ->
            val response = gateway.hentHistorikkStatusDetalj(
                SignifikantHistorikkRequest("12312312312", LocalDateTime.of(2025, 3, 27, 0, 0).toLocalDate())
            )

            assertThat(response).isEqualTo(
                HistorikkStatusDetaljResponse(
                    telleverk = TelleverkForPerson(7460, 460),
                    sisteSakOgVedtak = null,
                    sisteUtbetalingDato = null,
                )
            )
        }
    }

    @Test
    fun `Henter kvote og siste utbetalingsdato uten løpende vedtak`() {
        withTestServer(h2) { gateway ->
            val response = gateway.hentHistorikkStatusDetalj(
                SignifikantHistorikkRequest(kjentPerson, LocalDateTime.now().toLocalDate())
            )

            assertThat(response).isEqualTo(
                HistorikkStatusDetaljResponse(
                    telleverk = TelleverkForPerson(8800, null),
                    sisteSakOgVedtak = null,
                    sisteUtbetalingDato = LocalDate.of(2025, 4, 15),
                )
            )
        }
    }

    @Test
    fun `Henter tomme historikkstatusdetaljer for ukjent person`() {
        withTestServer(h2) { gateway ->
            val response = gateway.hentHistorikkStatusDetalj(
                SignifikantHistorikkRequest(ukjentPerson, LocalDateTime.now().toLocalDate())
            )

            assertThat(response).isEqualTo(HistorikkStatusDetaljResponse.ingen)
        }
    }

    @Test
    fun `henter unntaksstatus og maksdato for gammel prosedyre, eget vedtak og stansvedtak`() {
        val forventninger = listOf(
            Triple("gammel_nei", false, null),
            Triple("gammel_ja", true, null),
            Triple("eget_nei", false, null),
            Triple("eget_ja", true, null),
            Triple("stans_nei", false, null),
            Triple("stans_ja", true, null),
            Triple("gammel_dato", true, LocalDate.of(2025, 2, 1)),
        )

        withTestServer(h2) { gateway ->
            for ((person, innvilget, unntaksdato) in forventninger) {
                val response = gateway.hentHistorikkStatusDetalj(
                    SignifikantHistorikkRequest(person, testDato)
                )
                val detaljer = requireNotNull(response.sisteSakOgVedtak) { person }

                assertThat(detaljer.unntaksvilkaarInnvilget).describedAs(person).isEqualTo(innvilget)
                assertThat(detaljer.unntaksvilkaarGjelderFra).describedAs(person).isEqualTo(unntaksdato)
                assertThat(detaljer.sisteVedtak.maxdatoOrdinaer).describedAs(person)
                    .isEqualTo(LocalDate.of(2026, 1, 31))
                assertThat(detaljer.sisteVedtak.maxdatoUnntak).describedAs(person)
                    .isEqualTo(LocalDate.of(2027, 1, 31))
                assertThat(detaljer.sisteVedtak.maxdatoAap).describedAs(person)
                    .isEqualTo(LocalDate.of(2027, 1, 31))
            }
        }
    }

}
