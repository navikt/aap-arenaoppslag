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

    fun hentSamordningOgInstitusjon(saksopplysningerPerVedtak: Map<Int, List<ArenaSaksopplysning>>): Map<Int, SamordningOgInstitusjon> {
        return saksopplysningerPerVedtak.mapValues { (_, saksopplysninger) ->
            SamordningOgInstitusjon(
                institusjonOpphold = saksopplysninger.firstNotNullOfOrNull { it.tilInstitusjonOpphold() },
                andreYtelser = saksopplysninger.mapNotNull { it.tilAnnenYtelse() },
            )
        }
    }
}
