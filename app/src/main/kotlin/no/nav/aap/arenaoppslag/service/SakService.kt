package no.nav.aap.arenaoppslag.service

import com.github.benmanes.caffeine.cache.Caffeine
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics
import no.nav.aap.arenaoppslag.Metrics.prometheus
import no.nav.aap.arenaoppslag.database.SakRepository
import no.nav.aap.arenaoppslag.kontrakt.apiv1.MaksdatoResponse
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SakerResponse
import no.nav.aap.arenaoppslag.modeller.ArenaSakOppsummering

class SakService(private val sakRepository: SakRepository) {

    private val sakerCache = Caffeine.newBuilder()
        .maximumSize(10_000)
        .build<String, SakerResponse>()

    init {
        CaffeineCacheMetrics.monitor(prometheus, sakerCache, "arenaoppslag_saker_per_person")
    }

    fun hentSakerForPerson(fodselsnumre: Set<String>): SakerResponse {
        val cacheNokkel = fodselsnumre.sorted().joinToString(",")
        return sakerCache.get(cacheNokkel) {
            val saker: List<ArenaSakOppsummering> = sakRepository.hentSakerForPersnNummere(fodselsnumre)
            SakerResponse(saker = saker.map { it.tilKontrakt() })
        }
    }

    fun hentMaksdatoForSaker(sakidliste: List<Int>): MaksdatoResponse {
        val saker = sakRepository.finnMaksdatoer(sakidliste)
        return MaksdatoResponse(saker.map { it.tilKontrakt() })
    }

}
