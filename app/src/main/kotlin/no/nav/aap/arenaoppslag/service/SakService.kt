package no.nav.aap.arenaoppslag.service

import com.github.benmanes.caffeine.cache.Caffeine
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics
import no.nav.aap.arenaoppslag.Metrics.prometheus
import no.nav.aap.arenaoppslag.database.SakRepository
import no.nav.aap.arenaoppslag.database.VedtakfaktaRepository
import no.nav.aap.arenaoppslag.database.VilkårsvurderingRepository
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SakMedSisteVedtakOgMaksdato
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SakerResponse
import no.nav.aap.arenaoppslag.modeller.ArenaSakOppsummering
import no.nav.aap.arenaoppslag.modeller.PersonId
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class SakService(
    private val sakRepository: SakRepository,
    private val vedtakfaktaRepository: VedtakfaktaRepository,
    val vilkårsvurderingRepository: VilkårsvurderingRepository
) {

    @Suppress("MagicNumber")
    private val sakerCache = Caffeine.newBuilder()
        .maximumSize(10_000)
        .expireAfterAccess(30, TimeUnit.MINUTES)
        .build<String, SakerResponse>()

    init {
        CaffeineCacheMetrics.monitor(prometheus, sakerCache, "arenaoppslag_saker_per_person")
    }

    fun hentSakerForPerson(personId: PersonId): SakerResponse {
        val cacheNokkel = personId.id.toString()
        return sakerCache.get(cacheNokkel) {
            val saker: List<ArenaSakOppsummering> = sakRepository.hentSakerForPerson(personId)
            SakerResponse(saker = saker.map { it.tilKontrakt() })
        }
    }

    fun hentMaksdatoAapMedVedtakOgSak(personId: PersonId): SakMedSisteVedtakOgMaksdato? {
        val sakMedVedtak = sakRepository.hentMaxdatoForSisteVedtak(personId)

        val vedtakfakta = sakMedVedtak?.vedtakId?.let { vedtakId ->
            vedtakfaktaRepository.hentForVedtakIder(listOf(vedtakId))[vedtakId]
        }
        val vilkårsvurderingerForVedtaket = sakMedVedtak?.vedtakId?.let { vedtakId ->
            vilkårsvurderingRepository.hentForVedtakIder(listOf(vedtakId))[vedtakId]
        }

        val unntaksdatoFraFakta = vedtakfakta?.firstOrNull { it.kode == "AAPVILKUNN" }?.somDatoVerdi()

        // Korreksjon av Arena-data ved å bruke vilkårsvurderinger for å overstyre vedtakfakta
        val unntaksVilkår = setOf("AAARBEID1", "AAARBEID2", "AAARBEID3").map { kode ->
            vilkårsvurderingerForVedtaket?.firstOrNull { it.vilkårkode == kode }?.somBooleanVerdi()
        }
        val ingenVilkårOppfylt = unntaksVilkår.all { it == false }
        val minstEttVilkårOppfylt = unntaksVilkår.any { it == true }
        val (unntakInnvilget, unntaksdato) = when {
            ingenVilkårOppfylt -> false to null // dato er ugyldig når ingen av vilkårene er oppfylt
            minstEttVilkårOppfylt -> true to unntaksdatoFraFakta // bevarer dato, status er kjent
            else -> null to null // feil data, vi vet ikke status
        }

        return sakMedVedtak?.tilKontrakt()?.copy(
            unntaksvilkaarGjelderFra = unntaksdato, unntaksvilkaarInnvilget = unntakInnvilget
        )
    }

    /** Maksdato er funnet basert på reglen:
     * Finner siste AAP-vedtak for denne brukeren
     * Finner sak knyttet til dette vedtaket
     * Hvis løpende vedtak. Returner beregnet maksdato
     * Hvis sak som har gått til maksdato: Returner maksdato
     * Hvis siste vedtak er stansvedtak (S): Returnere null
     * Hvis vi ikke finner noen relevante saker: Returnere null
     */
    fun hentMaksdatoAapForPerson(personId: PersonId): LocalDate? {
        val sisteVedtak = hentMaksdatoAapMedVedtakOgSak(personId)?.sisteVedtak

        return sisteVedtak?.takeUnless { it.vedtaktypeKode == "S" }?.maxdatoAap
    }


}
