package no.nav.aap.arenaoppslag

import com.github.benmanes.caffeine.cache.Caffeine
import no.nav.aap.arenaoppslag.Metrics.registrerAntallSignifikanteVedtak
import no.nav.aap.arenaoppslag.Metrics.registrerSignifikantEnkeltVedtak
import no.nav.aap.arenaoppslag.Metrics.registrerSignifikantVedtak
import no.nav.aap.arenaoppslag.database.HistorikkRepository
import no.nav.aap.arenaoppslag.database.PersonRepository
import no.nav.aap.arenaoppslag.kontrakt.intern.Person
import no.nav.aap.arenaoppslag.kontrakt.intern.PersonEksistererIAAPArena
import no.nav.aap.arenaoppslag.kontrakt.intern.SignifikanteSakerResponse
import no.nav.aap.arenaoppslag.modeller.ArenaVedtak
import org.jetbrains.annotations.TestOnly
import java.time.LocalDate

class HistorikkService(
    private val personRepository: PersonRepository,
    private val historikkRepository: HistorikkRepository
) {

    @TestOnly
    fun hentAllePersoner(): List<Person> {
        return personRepository.hentAlle()
    }

    fun signifikanteSakerForPerson(
        fodselsnummerene: Set<String>, virkningstidspunkt: LocalDate
    ): SignifikanteSakerResponse {
        val personId: Int? = personRepository.hentPersonIdHvisEksisterer(fodselsnummerene.toSet())
        if (personId == null) {
            // early out
            return SignifikanteSakerResponse(harSignifikantHistorikk = false, signifikanteSaker = emptyList())
        }

        val signifikanteVedtak = historikkRepository.hentAlleSignifikanteVedtakForPerson(
            personId,
            virkningstidspunkt
        )
        rapporterMetrikker(signifikanteVedtak)

        val harSignifikantHistorikk = signifikanteVedtak.isNotEmpty()
        val arenaSakIdListe = sorterVedtak(signifikanteVedtak).map { it.sakId }.distinct()

        return SignifikanteSakerResponse(harSignifikantHistorikk, arenaSakIdListe)
    }

    private fun rapporterMetrikker(vedtakene: List<ArenaVedtak>) {
        vedtakene.forEach {
            Metrics.prometheus.registrerSignifikantVedtak(it)
        }

        if (vedtakene.size == 1) {
            // Bare ett vedtak hindret oss fra å ta inn personen inn i Kelvin
            Metrics.prometheus.registrerSignifikantEnkeltVedtak(vedtakene.first())
        }

        // Mål antall vedtak som hindret oss fra å ta personen inn i Kelvin
        Metrics.prometheus.registrerAntallSignifikanteVedtak(vedtakene.size)
    }

    internal fun sorterVedtak(vedtak: List<ArenaVedtak>): List<ArenaVedtak> {
        // Hvis saker uten tilDato finnes, sorter disse basert på db-order
        val utenSluttdato = vedtak.filter { it.tilDato == null }.reversed() // i reversed db-order (=nyeste først)
        // Hvis saker med tilDato finnes, sorter disse synkende på dato (=nyeste først)
        val medSluttdato = vedtak.filter { it.tilDato != null }.sortedByDescending { it.tilDato }
        return utenSluttdato + medSluttdato
    }

    // Lagrer mappingen fødselsnr -> arena-personId. Bare treff i databasen lagres.
    private val personIdCache = Caffeine.newBuilder().maximumSize(30_000).build<String, Int>()

    fun personEksistererIAapArena(fodselsnummerene: Set<String>): PersonEksistererIAAPArena {
        // prøv først cache, deretter gå til repository, deretter lagre det som evt. blir funnet i repository til cache
        val personId: Int? =
            fodselsnummerene.firstNotNullOfOrNull { personIdCache.getIfPresent(it) }
                ?: personRepository.hentPersonIdHvisEksisterer(fodselsnummerene)
                    ?.also { funnetPersonId ->
                        // lagre til cache
                        fodselsnummerene.forEach { personIdCache.put(it, funnetPersonId) }
                    }
        return PersonEksistererIAAPArena(personId != null)
    }

}
