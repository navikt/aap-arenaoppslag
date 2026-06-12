package no.nav.aap.arenaoppslag.service

import no.nav.aap.arenaoppslag.database.SaksopplysningRepository
import no.nav.aap.arenaoppslag.modeller.ArenaSaksopplysning
import no.nav.aap.arenaoppslag.modeller.SamordningOgInstitusjon
import no.nav.aap.arenaoppslag.modeller.tilAnnenYtelse
import no.nav.aap.arenaoppslag.modeller.tilInstitusjonOpphold

class SaksopplysningService(private val saksopplysningRepository: SaksopplysningRepository) {

    fun hentForVedtakId(vedtakId: Int): List<ArenaSaksopplysning> {
        return saksopplysningRepository.hentForVedtakId(vedtakId)
    }

    fun hentSamordningOgInstitusjon(areaSaksOpplysninger: List<ArenaSaksopplysning>): SamordningOgInstitusjon {
        return SamordningOgInstitusjon(
            institusjonOpphold = areaSaksOpplysninger.mapNotNull { it.tilInstitusjonOpphold() },
            andreYtelser = areaSaksOpplysninger.mapNotNull { it.tilAnnenYtelse() },
        )
    }
}
