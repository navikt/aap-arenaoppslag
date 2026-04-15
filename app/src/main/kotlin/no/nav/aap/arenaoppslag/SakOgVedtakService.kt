package no.nav.aap.arenaoppslag

import no.nav.aap.arenaoppslag.database.SakRepository
import no.nav.aap.arenaoppslag.database.VedtakRepository
import no.nav.aap.arenaoppslag.database.VedtakfaktaRepository
import no.nav.aap.arenaoppslag.modeller.ArenaSakMedVedtak
import no.nav.aap.arenaoppslag.modeller.toArenaSakMedVedtak

class SakOgVedtakService(
    private val sakRepository: SakRepository,
    private val vedtakRepository: VedtakRepository,
    private val vedtakfaktaRepository: VedtakfaktaRepository,
) {
    fun hentSakMedVedtak(saksId: Int): ArenaSakMedVedtak? {
        val sak = sakRepository.hentSak(saksId) ?: return null

        val vedtak = vedtakRepository.hentVedtakForSak(saksId)
        val faktaPerVedtak = vedtakfaktaRepository.hentForVedtakIder(vedtak.map { it.vedtakId })

        val vedtakMedFakta = vedtak.map { it.medFakta(faktaPerVedtak[it.vedtakId] ?: emptyList()) }

        return sak.toArenaSakMedVedtak(vedtakMedFakta)
    }
}
