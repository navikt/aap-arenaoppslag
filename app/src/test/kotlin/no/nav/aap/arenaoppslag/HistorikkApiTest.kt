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
import org.assertj.core.groups.Tuple.tuple
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class HistorikkApiTest : H2TestBase(
    "flyway/minimumtest", "flyway/eksisterer", "flyway/historikkapi", "flyway/historikkaktivitetsfase"
) {
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
            assertThat(response.signifikanteVedtak).extracting("rettighetkode", "vedtaktypeKode", "utfallkode")
                .containsExactly(tuple("AAP", "O", "JA"))
        }
    }

    @Test
    fun `signifikant historikk inneholder bare vanlige AAP-vedtak i UA og AU`() {
        withTestServer(h2) { gateway ->
            val response = gateway.harSignifikantHistorikk(
                SignifikantHistorikkRequest("aap_faser", testDato)
            )

            assertThat(response.harSignifikantHistorikk).isTrue
            assertThat(response.signifikanteVedtak).allMatch { it.rettighetkode == "AAP" }
            assertThat(response.signifikanteVedtak)
                .extracting("vedtaktypeKode", "statusKode", "fraOgMed", "utfallkode")
                .containsExactlyInAnyOrder(
                    tuple("O", "IVERK", LocalDate.of(2025, 1, 1), "JA"),
                    tuple("O", "IVERK", LocalDate.of(2025, 1, 2), "JA"),
                    tuple("E", "IVERK", LocalDate.of(2025, 1, 5), "JA"),
                    tuple("E", "IVERK", LocalDate.of(2025, 1, 6), "JA"),
                    tuple("G", "IVERK", LocalDate.of(2025, 1, 9), "JA"),
                    tuple("G", "IVERK", LocalDate.of(2025, 1, 10), "JA"),
                    tuple("O", "REGIS", null, null),
                    tuple("E", "REGIS", null, null)
                )
        }
    }

    @Test
    fun `kjent person med bare vanlige AAP-vedtak utenfor UA og AU har ikke signifikant historikk`() {
        withTestServer(h2) { gateway ->
            val person = "andre_faser"
            assertThat(gateway.harHistorikk(HarHistorikkRequest(person)).harHistorikk).isTrue

            val response = gateway.harSignifikantHistorikk(SignifikantHistorikkRequest(person, testDato))

            assertThat(response).isEqualTo(SignifikantHistorikkResponse.ingen)
        }
    }

    @Test
    fun `signifikant historikk beholder stanser med og uten sluttdato også utenfor UA og AU`() {
        withTestServer(h2) { gateway ->
            val response = gateway.harSignifikantHistorikk(
                SignifikantHistorikkRequest("stans_faser", testDato)
            )

            assertThat(response.harSignifikantHistorikk).isTrue
            assertThat(response.signifikanteVedtak).allMatch { it.vedtaktypeKode == "S" }
            assertThat(response.signifikanteVedtak).hasSize(8)
            assertThat(response.signifikanteVedtak.map { it.tilDato }).containsExactlyInAnyOrder(
                null, null, null, null,
                LocalDate.of(2025, 12, 31), LocalDate.of(2025, 12, 31),
                LocalDate.of(2025, 12, 31), LocalDate.of(2025, 12, 31)
            )
        }
    }

    @Test
    fun `signifikant historikk beholder ubehandlet AA115 utenfor UA og AU`() {
        withTestServer(h2) { gateway ->
            val response = gateway.harSignifikantHistorikk(SignifikantHistorikkRequest("aa115_fase", testDato))

            assertThat(response.harSignifikantHistorikk).isTrue
            assertThat(response.signifikanteVedtak).extracting("rettighetkode", "statusKode", "utfallkode")
                .containsExactly(tuple("AA115", "REGIS", null))
        }
    }

    @Test
    fun `signifikant historikk beholder klager og anker utenfor UA og AU`() {
        withTestServer(h2) { gateway ->
            val response = gateway.harSignifikantHistorikk(SignifikantHistorikkRequest("klager_anke", testDato))

            assertThat(response.harSignifikantHistorikk).isTrue
            assertThat(response.signifikanteVedtak.map { it.rettighetkode })
                .containsExactlyInAnyOrder("KLAG1", "KLAG2", "ANKE")
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

}
