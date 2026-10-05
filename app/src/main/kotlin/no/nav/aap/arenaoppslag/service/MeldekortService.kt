package no.nav.aap.arenaoppslag.service

import no.nav.aap.arenaoppslag.database.MeldekortRepository
import no.nav.aap.arenaoppslag.modeller.MeldekortStartuke
import no.nav.aap.arenaoppslag.modeller.PersonId

class MeldekortService(private val meldekortRepository: MeldekortRepository) {

    fun hentStartukeForSisteMeldekort(personId: PersonId): MeldekortStartuke? {
        return meldekortRepository.hentStartukeForSisteMeldekort(personId)
    }
}

