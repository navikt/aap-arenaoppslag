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

    @Test
    fun `Person har signifikant historikk i AAP-Arena`() {
        withTestServer(h2) { gateway ->
            val ukjentPerson: SignifikantHistorikkResponse = gateway.harSignifikantHistorikk(
                SignifikantHistorikkRequest(
                    kjentPerson,
                    LocalDateTime.now().minusDays(1).toLocalDate()
                )
            )

            assertThat(ukjentPerson.harSignifikantHistorikk).isTrue
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
    fun `Henter historikkstatusdetaljer for kjent person`() {
        withTestServer(h2) { gateway ->
            val response = gateway.hentHistorikkStatusDetalj(
                SignifikantHistorikkRequest("12312312312", LocalDateTime.of(2025, 3, 27, 0, 0).toLocalDate())
            )

            assertThat(response).isEqualTo(
                HistorikkStatusDetaljResponse(
                    telleverk = TelleverkForPerson(7460, null),
                    sisteSakDetalj = null,
                    sisteUtbetalingDato = null
                )
            )
        }
    }

    @Test
    fun `Henter kvote og siste utbetalingsdato for kjent person`() {
        withTestServer(h2) { gateway ->
            val response = gateway.hentHistorikkStatusDetalj(
                SignifikantHistorikkRequest(kjentPerson, LocalDateTime.now().toLocalDate())
            )

            assertThat(response.telleverk).isEqualTo(TelleverkForPerson(8800, null))
            assertThat(response.sisteUtbetalingDato).isEqualTo(LocalDate.of(2025, 4, 15))
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
