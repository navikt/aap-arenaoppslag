package no.nav.aap.arenaoppslag.service

import no.nav.aap.arenaoppslag.database.SaksopplysningRepository
import no.nav.aap.arenaoppslag.modeller.ArenaSaksopplysning
import no.nav.aap.arenaoppslag.modeller.SamordningMedInstitusjon
import no.nav.aap.arenaoppslag.modeller.tilAnnenYtelse
import no.nav.aap.arenaoppslag.modeller.tilInstitusjonOpphold

class SaksopplysningService(private val saksopplysningRepository: SaksopplysningRepository) {

    fun hentForVedtakId(vedtakId: Int): List<ArenaSaksopplysning> {
        return saksopplysningRepository.hentForVedtakId(vedtakId)
    }

    fun hentForVedtakIder(vedtakIder: List<Int>): Map<Int, List<ArenaSaksopplysning>> {
        return saksopplysningRepository.hentForVedtakIder(vedtakIder)
    }

    fun hentSamordningOgInstitusjon(saksopplysningerPerVedtak: Map<Int, List<ArenaSaksopplysning>>): Map<Int, SamordningMedInstitusjon> {
        return saksopplysningerPerVedtak.mapValues { (_, saksopplysninger) ->
            SamordningMedInstitusjon(
                institusjonOpphold = saksopplysninger.firstNotNullOfOrNull { it.tilInstitusjonOpphold() },
                andreYtelser = saksopplysninger.mapNotNull { it.tilAnnenYtelse() },
            )
        }
    }
}
