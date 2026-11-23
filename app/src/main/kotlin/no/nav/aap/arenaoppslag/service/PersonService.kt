package no.nav.aap.arenaoppslag.service

import com.github.benmanes.caffeine.cache.Caffeine
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics
import no.nav.aap.arenaoppslag.Metrics.prometheus
import no.nav.aap.arenaoppslag.database.PersonRepository
import no.nav.aap.arenaoppslag.database.SakRepository
import no.nav.aap.arenaoppslag.modeller.ArenaPersonId
import no.nav.aap.arenaoppslag.modeller.PersonIdentifikator
import no.nav.aap.arenaoppslag.modeller.Saksnummer
import no.nav.aap.arenaoppslag.pdl.IPdlGateway

class PersonService(
    private val personRepository: PersonRepository,
    private val sakRepository: SakRepository,
    private val pdlGateway: IPdlGateway,
) {
    // Lagrer mappingen personidentifikator -> arena-personId. Bare treff i databasen lagres.
    @Suppress("MagicNumber")
    private val personIdCache = Caffeine.newBuilder()
        .maximumSize(30_000)
        .recordStats()
        .build<PersonIdentifikator, ArenaPersonId>()

    // Lagrer mappingen arena-saksnummer -> personidentifikator . Bare treff i databasen lagres.
    @Suppress("MagicNumber")
    private val saksnummerTilPersonidentifikatorCache = Caffeine.newBuilder()
        .maximumSize(30_000)
        .recordStats()
        .build<Saksnummer, PersonIdentifikator>()

    init {
        CaffeineCacheMetrics.monitor(prometheus, personIdCache, "arenaoppslag_person_id")
        CaffeineCacheMetrics.monitor(
            prometheus,
            saksnummerTilPersonidentifikatorCache,
            "arenaoppslag_saksnummer_til_personidentifikator",
        )
    }

    fun hentPersonIdentifikator(personidentifikator: PersonIdentifikator): ArenaPersonId? {
        return personIdCache.getIfPresent(personidentifikator)
            ?: hentArenaPersonIdUtenCache(personidentifikator)
                ?.also { funnetPersonId ->
                    personIdCache.put(personidentifikator, funnetPersonId)
                }
    }

    private fun hentArenaPersonIdUtenCache(personidentifikator: PersonIdentifikator): ArenaPersonId? {
        val alleIdenter = pdlGateway.hentAlleIdenterForPerson(personidentifikator)
            .map { PersonIdentifikator(it.ident) }
            .toSet()
        return personRepository.hentPersonIdHvisEksisterer(alleIdenter)
    }

    fun hentPersonIdentifikator(saknummer: Saksnummer): PersonIdentifikator? {
        return saksnummerTilPersonidentifikatorCache.getIfPresent(saknummer)
            ?: hentPersonIdentifikatorUtenCache(saknummer)
                ?.also { funnetPersonId ->
                    saksnummerTilPersonidentifikatorCache.put(saknummer, funnetPersonId)
                }
    }

    private fun hentPersonIdentifikatorUtenCache(saknummer: Saksnummer): PersonIdentifikator? {
        return sakRepository.hentPersonIdentifikatorHvisEksisterer(saknummer)
    }

}
