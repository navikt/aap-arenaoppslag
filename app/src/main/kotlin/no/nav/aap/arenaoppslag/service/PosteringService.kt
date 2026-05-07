package no.nav.aap.arenaoppslag.service

import no.nav.aap.arenaoppslag.database.PosteringRepository
import java.time.LocalDate

class PosteringService(private val posteringRepository: PosteringRepository) {

    fun hentSisteUtbetalingISak(saksId: Int): LocalDate? {
        return posteringRepository.hentNyestePosteringISak(saksId)
    }

}
