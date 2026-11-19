package no.nav.aap.arenaoppslag.service

import no.nav.aap.arenaoppslag.database.MeldekortRepository
import no.nav.aap.arenaoppslag.modeller.MeldekortStartuke
import no.nav.aap.arenaoppslag.modeller.ArenaPersonId

class MeldekortService(private val meldekortRepository: MeldekortRepository) {

    fun hentStartukeForSisteMeldekort(personId: ArenaPersonId): MeldekortStartuke? {
        return meldekortRepository.hentStartukeForSisteMeldekort(personId)
    }
}

