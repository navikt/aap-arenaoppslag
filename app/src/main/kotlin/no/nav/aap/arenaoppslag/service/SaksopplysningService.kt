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
        // Saksopplysninger tilhører en sak men kobles til vedtak via LOV_VEDTAK_SAKSOPPLYSNING,
        // så samme post kan dukke opp flere ganger når vi samler via flatMap over vedtak.
        val unike = areaSaksOpplysninger.distinctBy { it.saksopplysningId }
        return SamordningOgInstitusjon(
            institusjonOpphold = unike.mapNotNull { it.tilInstitusjonOpphold() },
            andreYtelser = unike.mapNotNull { it.tilAnnenYtelse() },
        )
    }
}
