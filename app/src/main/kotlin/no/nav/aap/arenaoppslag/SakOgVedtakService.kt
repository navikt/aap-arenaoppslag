package no.nav.aap.arenaoppslag

import no.nav.aap.arenaoppslag.database.SakRepository
import no.nav.aap.arenaoppslag.database.VedtakRepository
import no.nav.aap.arenaoppslag.database.VedtakfaktaRepository
import no.nav.aap.arenaoppslag.database.VilkårsvurderingRepository
import no.nav.aap.arenaoppslag.modeller.ArenaSak
import no.nav.aap.arenaoppslag.modeller.ArenaSakMedVedtak
import no.nav.aap.arenaoppslag.modeller.ArenaVedtak
import no.nav.aap.arenaoppslag.modeller.ArenaVedtakMedDetaljer
import no.nav.aap.arenaoppslag.modeller.SakId
import no.nav.aap.arenaoppslag.modeller.Saksnummer
import no.nav.aap.arenaoppslag.modeller.toArenaSakMedVedtak
import no.nav.aap.arenaoppslag.tilgangsmaskin.AuthorizedPersonId

class SakOgVedtakService(
    private val sakRepository: SakRepository,
    private val vedtakRepository: VedtakRepository,
    private val vedtakfaktaRepository: VedtakfaktaRepository,
    private val vilkårsvurderingRepository: VilkårsvurderingRepository,
) {
    fun hentSakMedVedtak(saksId: SakId): ArenaSakMedVedtak? {
        val sak = sakRepository.hentSak(saksId) ?: return null
        return getArenaSakMedVedtak(sak)
    }

    fun hentSakMedVedtak(saksnummer: Saksnummer): ArenaSakMedVedtak? {
        val sak = sakRepository.hentSak(saksnummer) ?: return null
        return getArenaSakMedVedtak(sak)
    }

    context(authorized: AuthorizedPersonId)
    fun hentVedtakForPerson(): List<ArenaVedtak> {
        return vedtakRepository.hentVedtak(authorized.personId)
    }

    context(authorized: AuthorizedPersonId)
    fun hentVedtakDetaljerForPerson(): List<ArenaVedtakMedDetaljer> {
        val saker = sakRepository.hentSakerDetaljerForPerson(authorized.personId)
        return saker.flatMap { sak -> getArenaSakMedVedtak(sak).vedtak }
    }

    private fun getArenaSakMedVedtak(sak: ArenaSak): ArenaSakMedVedtak {
        val vedtak = vedtakRepository.hentVedtakForSak(SakId(sak.sakId.toInt()))
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
