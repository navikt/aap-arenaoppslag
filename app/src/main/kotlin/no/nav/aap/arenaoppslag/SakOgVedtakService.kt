package no.nav.aap.arenaoppslag

import no.nav.aap.arenaoppslag.database.SakRepository
import no.nav.aap.arenaoppslag.database.VedtakRepository
import no.nav.aap.arenaoppslag.database.VedtakfaktaRepository
import no.nav.aap.arenaoppslag.database.VilkårsvurderingRepository
import no.nav.aap.arenaoppslag.modeller.ArenaSakMedVedtak
import no.nav.aap.arenaoppslag.modeller.toArenaSakMedVedtak

class SakOgVedtakService(
    private val sakRepository: SakRepository,
    private val vedtakRepository: VedtakRepository,
    private val vedtakfaktaRepository: VedtakfaktaRepository,
    private val vilkårsvurderingRepository: VilkårsvurderingRepository,
) {
    fun hentSakMedVedtak(saksId: Int): ArenaSakMedVedtak? {
        val sak = sakRepository.hentSak(saksId) ?: return null

        val vedtak = vedtakRepository.hentVedtakForSak(saksId)
        val vedtakIder = vedtak.map { it.vedtakId }

        val faktaPerVedtak = vedtakfaktaRepository.hentForVedtakIder(vedtakIder)
        val vilkårsvurderingerPerVedtak = vilkårsvurderingRepository.hentForVedtakIder(vedtakIder)

        val vedtakMedFaktaOgVilkår = vedtak.map { v ->
            v.medFakta(
                fakta = faktaPerVedtak[v.vedtakId] ?: emptyList(),
                vilkårsvurderinger = vilkårsvurderingerPerVedtak[v.vedtakId] ?: emptyList(),
            )
        }

        return sak.toArenaSakMedVedtak(vedtakMedFaktaOgVilkår)
    }
}
