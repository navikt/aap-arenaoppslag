package no.nav.aap.arenaoppslag.service

import com.github.benmanes.caffeine.cache.Caffeine
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics
import no.nav.aap.arenaoppslag.Metrics.prometheus
import no.nav.aap.arenaoppslag.database.SakRepository
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SakMedSisteVedtakOgMaksdato
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SakerResponse
import no.nav.aap.arenaoppslag.modeller.ArenaSakOppsummering
import no.nav.aap.arenaoppslag.modeller.PersonId
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class SakService(private val sakRepository: SakRepository) {

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
        return sakMedVedtak?.tilKontrakt()
    }

    /** Maksdato er funnet basert på reglen:
     * Finner siste AAP-vedtak for denne brukeren
     * Finner sak knyttet til dette vedtaket
     * Hvis løpende vedtak. Returner beregnet maksdato
     * Hvis sak som har gått til maks: Returner maksdato
     * Hvis siste vedtak er stansvedtak (S): Returnere null
     * Hvis vi ikke finner noen relevante saker: Returnere null
     */
    fun hentMaksdatoAapForPerson(personId: PersonId): LocalDate? {
        val sisteVedtak = hentMaksdatoAapMedVedtakOgSak(personId)?.sisteVedtak

        return sisteVedtak
            ?.takeUnless { it.vedtaktypeKode == "S" }
            ?.maxdatoAap
    }


}
