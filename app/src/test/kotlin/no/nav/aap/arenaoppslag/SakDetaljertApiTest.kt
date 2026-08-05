package no.nav.aap.arenaoppslag

import no.nav.aap.arenaoppslag.client.ArenaOppslagGateway.Companion.withTestServer
import no.nav.aap.arenaoppslag.database.H2TestBase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SakDetaljertApiTest : H2TestBase("flyway/minimumtest", "flyway/oppgave") {

    @Test
    fun `Henter oppgaver for personen bak saken`() {
        withTestServer(h2) { gateway ->
            val sak = gateway.hentSakDetaljert("1")

            assertThat(sak.sakId).isEqualTo("1")
            assertThat(sak.oppgaver.map { it.fristDato })
                .containsExactly(LocalDate.of(2024, 9, 15), LocalDate.of(2024, 5, 1), null)
            assertThat(sak.oppgaver.first().beskrivelse).isEqualTo("Behandle meldekort")
        }
    }

    @Test
    fun `Returnerer tom oppgaveliste for person uten oppgaver`() {
        withTestServer(h2) { gateway ->
            val sak = gateway.hentSakDetaljert("9")

            assertThat(sak.oppgaver).isEmpty()
        }
    }
}


