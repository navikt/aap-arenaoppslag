package no.nav.aap.arenaoppslag

import no.nav.aap.arenaoppslag.database.SakRepository
import no.nav.aap.arenaoppslag.database.VedtakRepository
import no.nav.aap.arenaoppslag.modeller.ArenaSak
import no.nav.aap.arenaoppslag.modeller.ArenaSakMedVedtak
import no.nav.aap.arenaoppslag.modeller.ArenaVedtak
import no.nav.aap.arenaoppslag.modeller.toArenaSakMedVedtak

class SakService(
    private val sakRepository: SakRepository,
    private val vedtakRepository: VedtakRepository
) {
    fun hentSakMedVedtak(saksId: Int): ArenaSakMedVedtak? {
        val sak = sakRepository.hentSak(saksId)
        val vedtakForSak = vedtakRepository.hentVedtakMedFaktaForSak(saksId)

        return sak?.toArenaSakMedVedtak(vedtakForSak)
    }
}