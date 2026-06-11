package no.nav.aap.arenaoppslag.service

import no.nav.aap.arenaoppslag.database.SaksopplysningRepository
import no.nav.aap.arenaoppslag.modeller.ArenaSaksopplysning

class SaksopplysningService(private val saksopplysningRepository: SaksopplysningRepository) {

    fun hentForVedtakId(vedtakId: Int): List<ArenaSaksopplysning> {
        return saksopplysningRepository.hentForVedtakId(vedtakId)
    }
}

