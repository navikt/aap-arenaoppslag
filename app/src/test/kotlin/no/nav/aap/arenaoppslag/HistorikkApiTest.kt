package no.nav.aap.arenaoppslag

import no.nav.aap.arenaoppslag.client.ArenaOppslagGateway.Companion.withTestServer
import no.nav.aap.arenaoppslag.database.H2TestBase
import no.nav.aap.arenaoppslag.kontrakt.apiv1.HarHistorikkRequest
import no.nav.aap.arenaoppslag.kontrakt.apiv1.HarHistorikkResponse
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SignifikantHistorikkRequest
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SignifikantHistorikkResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class HistorikkApiTest : H2TestBase("flyway/minimumtest", "flyway/eksisterer") {
    companion object {
        const val ukjentPerson = "007"
        const val kjentPerson = "kun_nye"
    }

    private val testDato = LocalDate.parse("2025-06-15")

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

}
