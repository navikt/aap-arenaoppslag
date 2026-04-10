package no.nav.aap.arenaoppslag.service

import com.github.benmanes.caffeine.cache.Caffeine
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics
import no.nav.aap.arenaoppslag.Metrics.prometheus
import no.nav.aap.arenaoppslag.database.SakRepository
import no.nav.aap.arenaoppslag.kontrakt.apiv1.ArenaSak
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SakerResponse

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
            val saker: List<ArenaSak> = sakRepository.hentSakerForPersoner(fodselsnumre)
            SakerResponse(saker = saker)
        }
    }
}
