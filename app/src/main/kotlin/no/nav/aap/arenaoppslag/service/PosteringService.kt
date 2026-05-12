package no.nav.aap.arenaoppslag.service

import no.nav.aap.arenaoppslag.database.PosteringRepository
import no.nav.aap.arenaoppslag.modeller.PersonId
import java.time.LocalDate

class PosteringService(private val posteringRepository: PosteringRepository) {

    fun hentSisteAapUtbetalingForPerson(personId: PersonId): LocalDate? {
        return posteringRepository.hentSisteAapUtbetalingForPerson(personId)
    }

}
