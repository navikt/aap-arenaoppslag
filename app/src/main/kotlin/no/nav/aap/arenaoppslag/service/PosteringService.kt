package no.nav.aap.arenaoppslag.service

import no.nav.aap.arenaoppslag.database.PosteringRepository
import no.nav.aap.arenaoppslag.modeller.ArenaPersonId
import java.time.LocalDate

class PosteringService(private val posteringRepository: PosteringRepository) {

    fun hentSisteAapUtbetalingForPerson(personId: ArenaPersonId): LocalDate? {
        return posteringRepository.hentSisteAapUtbetalingForPerson(personId)
    }

}
