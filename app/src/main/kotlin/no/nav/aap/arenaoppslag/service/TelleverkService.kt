package no.nav.aap.arenaoppslag.service

import com.github.benmanes.caffeine.cache.Caffeine
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics
import no.nav.aap.arenaoppslag.Metrics.prometheus
import no.nav.aap.arenaoppslag.database.TelleverkRepository
import no.nav.aap.arenaoppslag.modeller.KvotebrukHendelse
import no.nav.aap.arenaoppslag.modeller.PersonId
import no.nav.aap.arenaoppslag.modeller.TelleverkForPerson
import java.time.Duration

@Suppress("MagicNumber")
class TelleverkService(private val telleverkRepository: TelleverkRepository) {

    private val telleverkCache = Caffeine.newBuilder()
        .maximumSize(10_000)
        .expireAfterWrite(Duration.ofMinutes(15))
        .build<Int, TelleverkForPerson>()

    private val kvotebrukCache = Caffeine.newBuilder()
        .maximumSize(10_000)
        .expireAfterWrite(Duration.ofMinutes(15))
        .build<Int, Set<KvotebrukHendelse>>()

    init {
        CaffeineCacheMetrics.monitor(prometheus, telleverkCache, "arenaoppslag_telleverk_per_person")
        CaffeineCacheMetrics.monitor(prometheus, kvotebrukCache, "arenaoppslag_kvotebruk_per_person")
    }

    // Caffeine kan ikke lagre null, så personer uten telleverk gir nytt databaseoppslag hver gang.
    fun hentTelleverkForPerson(personId: PersonId): TelleverkForPerson? =
        telleverkCache.getIfPresent(personId.id)
            ?: hentTelleverkForPersonUtenCache(personId)?.also { telleverkCache.put(personId.id, it) }

    private fun hentTelleverkForPersonUtenCache(personId: PersonId): TelleverkForPerson? {
        val tellekvoter = telleverkRepository.hentTelleverkForPerson(personId)
        val ordinaerAAPKvote = tellekvoter.firstOrNull { it.kode == "AAP" }?.verdi
        val utvidetAAPKvote = tellekvoter.firstOrNull { it.kode == "MAAPU" }?.verdi

        // ordinaerAAPKvote skal finnes i alle tilfeller. Men det er enkelte unntak for hvis personen ikke har
        // telleverk i det hele tatt. F.eks. fordi det ikke er fattet noen vedtak på saken til personen.
        if (ordinaerAAPKvote == null) {
            return null
        }

        return TelleverkForPerson(
            ordineerAAPKvote = ordinaerAAPKvote,
            utvidetAAPKvote = utvidetAAPKvote
        )
    }

    fun hentKvoteBrukHendelserForPerson(personId: PersonId): Set<KvotebrukHendelse> =
        kvotebrukCache.get(personId.id) {
            telleverkRepository.hentKvoteBrukHendelserForPerson(personId)
        }

}
