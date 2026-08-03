package no.nav.aap.arenaoppslag.database

import no.nav.aap.arenaoppslag.modeller.PersonId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class OppgaveRepositoryTest : H2TestBase("flyway/minimumtest", "flyway/oppgave") {

    @Test
    fun `hentOppgaverForPerson returnerer oppgaver sortert paa frist synkende med tomme frister sist`() {
        val oppgaveRepository = OppgaveRepository(h2)

        val oppgaver = oppgaveRepository.hentOppgaverForPerson(PersonId(1))

        assertThat(oppgaver).hasSize(3)
        assertThat(oppgaver.map { it.fristDato })
            .containsExactly(LocalDate.of(2024, 9, 15), LocalDate.of(2024, 5, 1), null)
    }

    @Test
    fun `hentOppgaverForPerson mapper alle felter`() {
        val oppgaveRepository = OppgaveRepository(h2)

        val oppgave = oppgaveRepository.hentOppgaverForPerson(PersonId(1))
            .single { it.fristDato == LocalDate.of(2024, 5, 1) }

        assertThat(oppgave.beskrivelse).isEqualTo("Vurder rett til AAP")
        assertThat(oppgave.sakskontekst).isEqualTo("AA")
        assertThat(oppgave.visningsnavn).isEqualTo("Vurder rettighet")
        assertThat(oppgave.arbeidsbenk).isEqualTo("Min benk")
        assertThat(oppgave.oppgaveEnhet).isEqualTo("0826")
        assertThat(oppgave.navEnhet).isEqualTo("4402")
        assertThat(oppgave.notat).isEqualTo("Notat om oppgaven")
    }

    @Test
    fun `hentOppgaverForPerson taaler oppgave uten utfylte felter`() {
        val oppgaveRepository = OppgaveRepository(h2)

        val oppgave = oppgaveRepository.hentOppgaverForPerson(PersonId(1)).last()

        assertThat(oppgave.beskrivelse).isEqualTo("Oppgave uten frist")
        assertThat(oppgave.sakskontekst).isNull()
        assertThat(oppgave.visningsnavn).isNull()
        assertThat(oppgave.fristDato).isNull()
        assertThat(oppgave.arbeidsbenk).isNull()
        assertThat(oppgave.oppgaveEnhet).isNull()
        assertThat(oppgave.navEnhet).isNull()
        assertThat(oppgave.notat).isNull()
    }

    @Test
    fun `hentOppgaverForPerson returnerer kun oppgaver for etterspurt person`() {
        val oppgaveRepository = OppgaveRepository(h2)

        val oppgaver = oppgaveRepository.hentOppgaverForPerson(PersonId(2))

        assertThat(oppgaver).singleElement()
            .satisfies({ oppgave -> assertThat(oppgave.beskrivelse).isEqualTo("Oppgave for annen person") })
    }

    @Test
    fun `hentOppgaverForPerson returnerer tom liste for ukjent person`() {
        val oppgaveRepository = OppgaveRepository(h2)

        assertThat(oppgaveRepository.hentOppgaverForPerson(PersonId(999))).isEmpty()
    }
}

