package no.nav.aap.arenaoppslag

import io.ktor.http.HttpStatusCode
import no.nav.aap.arenaoppslag.client.ArenaOppslagGateway.Companion.withTestServer
import no.nav.aap.arenaoppslag.database.H2TestBase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

// Oppgaver eksponeres av /api/intern/sak/{sakid}/oppgaver.
class OppgaverForSakApiTest : H2TestBase("flyway/minimumtest", "flyway/oppgave") {

    @Test
    fun `Henter oppgaver for personen bak saken`() {
        withTestServer(h2) { gateway ->
            val oppgaver = gateway.hentOppgaver("2021-1")

            assertThat(oppgaver.map { it.fristDato })
                .containsExactly(LocalDate.of(2024, 9, 15), LocalDate.of(2024, 5, 1), null)
            assertThat(oppgaver.first().beskrivelse).isEqualTo("Behandle meldekort")
        }
    }

    @Test
    fun `Returnerer tom oppgaveliste for person uten oppgaver`() {
        withTestServer(h2) { gateway ->
            val oppgaver = gateway.hentOppgaver("2023-9")

            assertThat(oppgaver).isEmpty()
        }
    }

    @Test
    fun `ugyldig saksnummerformat gir 400`() {
        withTestServer(h2) { gateway ->
            assertThat(gateway.hentOppgaverStatus("99999")).isEqualTo(HttpStatusCode.BadRequest)
        }
    }

    @Test
    fun `ukjent sak gir 404`() {
        withTestServer(h2) { gateway ->
            assertThat(gateway.hentOppgaverStatus("2023-99999")).isEqualTo(HttpStatusCode.NotFound)
        }
    }
}
