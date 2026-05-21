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

    fun hentMaksdatoAapForVedtakISaker(personId: PersonId): List<SakMedSisteVedtakOgMaksdato> {
        val sakerMedVedtak = sakRepository.hentSakerMedMaksDatoOgVedtak(personId)
        return sakerMedVedtak.map { it.tilKontrakt() }
    }

    fun hentMaksdatoAapForPerson(personId: PersonId): LocalDate? {
        val maksdatoene = hentMaksdatoAapForVedtakISaker(personId)
        val sisteSak = maksdatoene
            .filter { it.sisteVedtak.maxdatoAap != null }.maxBy { it.sisteVedtak.maxdatoAap!! }
        /** Maksdato er funner basert på reglen:
         * Finner siste AAP-vedtak for denne brukeren
         * Finner sak knytet til dette vedtaket
         * Hvis løpende vedtak. Returner beregnet maksdato
         * Hvis sak som har gått til maks: Returner maksdato
         * Hvis siste vedtak er stansvedtak: Returnere null
         * Hvis vi ikke finner noen relevante saker: Returnere null
         */


        if (!sisteSak.lopendeVedtak) return null

        return sisteSak.sisteVedtak.maxdatoAap
    }

}
