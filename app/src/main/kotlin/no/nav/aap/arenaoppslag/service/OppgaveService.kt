package no.nav.aap.arenaoppslag.service

import no.nav.aap.arenaoppslag.database.OppgaveRepository
import no.nav.aap.arenaoppslag.modeller.ArenaOppgave
import no.nav.aap.arenaoppslag.modeller.ArenaPersonId

class OppgaveService(private val oppgaveRepository: OppgaveRepository) {
    fun hentOppgaverForPerson(personId: ArenaPersonId): List<ArenaOppgave> =
        oppgaveRepository.hentOppgaverForPerson(personId)
}

