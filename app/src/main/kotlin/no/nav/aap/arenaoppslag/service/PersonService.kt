package no.nav.aap.arenaoppslag.service

import com.github.benmanes.caffeine.cache.Caffeine
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics
import no.nav.aap.arenaoppslag.Metrics.prometheus
import no.nav.aap.arenaoppslag.database.PersonRepository
import no.nav.aap.arenaoppslag.modeller.PersonId
import no.nav.aap.arenaoppslag.pdl.IPdlGateway

class PersonService(
    private val personRepository: PersonRepository,
    private val pdlGateway: IPdlGateway,
) {
    // Lagrer mappingen personidentifikator -> arena-personId. Bare treff i databasen lagres.
    @Suppress("MagicNumber")
    private val personIdCache = Caffeine.newBuilder()
        .maximumSize(30_000)
        .recordStats()
        .build<String, PersonId>()

    init {
        CaffeineCacheMetrics.monitor(prometheus, personIdCache, "arenaoppslag_person_id")
    }

    fun hentPersonId(personidentifikator: String): PersonId? {
        return personIdCache.getIfPresent(personidentifikator)
            ?: hentPersonIdUtenCache(personidentifikator)
                ?.also { funnetPersonId ->
                    personIdCache.put(personidentifikator, funnetPersonId)
                }
    }

    private fun hentPersonIdUtenCache(personidentifikator: String): PersonId? {
        val alleIdenter = pdlGateway.hentAlleIdenterForPerson(personidentifikator)
            .map { it.ident }
            .toSet()
        return personRepository.hentPersonIdHvisEksisterer(alleIdenter)
    }
}
