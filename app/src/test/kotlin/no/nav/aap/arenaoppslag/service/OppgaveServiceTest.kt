package no.nav.aap.arenaoppslag.service

import io.mockk.every
import io.mockk.mockk
import no.nav.aap.arenaoppslag.database.OppgaveRepository
import no.nav.aap.arenaoppslag.modeller.ArenaOppgave
import no.nav.aap.arenaoppslag.modeller.PersonId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class OppgaveServiceTest {

    private val repo = mockk<OppgaveRepository>()
    private val service = OppgaveService(repo)

    @Test
    fun `hentOppgaverForPerson returnerer oppgavene fra repository`() {
        val oppgave = ArenaOppgave(
            oppgaveId = 1L,
            beskrivelse = "Vurder rett til AAP",
            sakskontekst = "AA",
            visningsnavn = "Vurder rettighet",
            fristDato = LocalDate.of(2024, 5, 1),
            arbeidsbenk = "Min benk",
            oppgaveEnhet = "0826",
            navEnhet = "4402",
            notat = null,
        )
        every { repo.hentOppgaverForPerson(PersonId(1)) } returns listOf(oppgave)

        assertThat(service.hentOppgaverForPerson(PersonId(1))).containsExactly(oppgave)
    }

    @Test
    fun `hentOppgaverForPerson returnerer tom liste naar personen ikke har oppgaver`() {
        every { repo.hentOppgaverForPerson(PersonId(2)) } returns emptyList()

        assertThat(service.hentOppgaverForPerson(PersonId(2))).isEmpty()
    }
}

