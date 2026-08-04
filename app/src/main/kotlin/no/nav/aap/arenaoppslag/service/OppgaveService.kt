package no.nav.aap.arenaoppslag.service

import no.nav.aap.arenaoppslag.database.OppgaveRepository
import no.nav.aap.arenaoppslag.modeller.ArenaOppgave
import no.nav.aap.arenaoppslag.modeller.PersonId

class OppgaveService(private val oppgaveRepository: OppgaveRepository) {
    fun hentOppgaverForPerson(personId: PersonId): List<ArenaOppgave> =
        oppgaveRepository.hentOppgaverForPerson(personId)
}

