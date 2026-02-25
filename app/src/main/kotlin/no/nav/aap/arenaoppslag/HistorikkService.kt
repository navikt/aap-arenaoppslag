package no.nav.aap.arenaoppslag

import com.github.benmanes.caffeine.cache.Caffeine
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import no.nav.aap.arenaoppslag.database.HistorikkRepository
import no.nav.aap.arenaoppslag.database.PersonRepository
import no.nav.aap.arenaoppslag.kontrakt.intern.NyereSakerResponse
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
        personIdentifikatorer: List<String>, virkningstidspunkt: LocalDate
    ): SignifikanteSakerResponse {
        val personId: Int? = personRepository.hentPersonIdHvisEksisterer(personIdentifikatorer.toSet())
        if (personId == null) {
            // Personen finnes ikke i AAP-Arena i det hele tatt
            return SignifikanteSakerResponse(harSignifikantHistorikk = false, signifikanteSaker = emptyList())
        }

        val signifikanteVedtak = historikkRepository.hentAlleSignifikanteVedtakForPerson(
            personId,
            virkningstidspunkt
        )
        rapporterMetrikkerForSignifikanteVedtak(signifikanteVedtak)

        val harSignifikantHistorikk = signifikanteVedtak.isNotEmpty()
        val arenaSakIdListe = sorterVedtak(signifikanteVedtak).map { it.sakId }.distinct()
        // TODO Vurder å slippe inn søknader med 11-5-vedtak som er åpne uten at det er et AAP-pengevedtak
        //  i samme periode, forutsatt at disse 11-5 vedtakene ikke er så nye at AAP-vedtaket ikke er laget enda.

        return SignifikanteSakerResponse(harSignifikantHistorikk, arenaSakIdListe)
    }

    private fun rapporterMetrikkerForSignifikanteVedtak(vedtakene: List<ArenaVedtak>) {
        vedtakene.forEach {
            Metrics.prometheus.registrerSignifikantVedtak(it)
        }
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

    fun personEksistererIAapArena(personidentifikatorer: Set<String>): PersonEksistererIAAPArena {
        // prøv først cache, deretter gå til repository, deretter lagre det som evt. blir funnet i repository til cache
        val personId: Int? =
            personidentifikatorer.firstNotNullOfOrNull { personIdCache.getIfPresent(it) }
                ?: personRepository.hentPersonIdHvisEksisterer(personidentifikatorer)
                    ?.also { funnetPersonId ->
                        // lagre til cache
                        personidentifikatorer.forEach { personIdCache.put(it, funnetPersonId) }
                    }
        return PersonEksistererIAAPArena(personId != null)
    }

    fun personHarNyereHistorikk(personidentifikatorer: List<String>): NyereSakerResponse {
        val personId: Int? = personRepository.hentPersonIdHvisEksisterer(personidentifikatorer.toSet())
        if (personId == null) {
            // early out
            return NyereSakerResponse(false, emptyList())
        }
        val nyereVedtak = historikkRepository.`hentIkkeAvbrutteVedtakSisteFemÅrForPerson`(personId)

        val harNyereHistorikk = nyereVedtak.isNotEmpty()
        val arenaSakIdListe = sorterVedtak(nyereVedtak).map { it.sakId }.distinct()

        return NyereSakerResponse(harNyereHistorikk, arenaSakIdListe)
    }

    fun MeterRegistry.registrerSignifikantVedtak(vedtak: ArenaVedtak) {
        this.counter(
            "arenaoppslag_signifikant_vedtak",
            listOf(
                Tag.of("type", vedtak.vedtaktypeKode ?: "null"),
                Tag.of("rettighet", vedtak.rettighetkode),
                Tag.of("status", vedtak.statusKode),
                Tag.of("utfall", vedtak.utfallkode ?: "null")
            )
        ).also { counter -> counter.increment() }
    }
}
