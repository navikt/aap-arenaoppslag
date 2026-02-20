package no.nav.aap.arenaoppslag

import io.micrometer.core.instrument.Counter
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
            // early out
            return SignifikanteSakerResponse(harSignifikantHistorikk = false, signifikanteSaker = emptyList())
        }

        val signifikanteVedtak = historikkRepository.hentAlleSignifikanteVedtakForPerson(
            personId,
            virkningstidspunkt
        )
        rapporterMetrikkerForSignifikanteVedtak(signifikanteVedtak)

        val harSignifikantHistorikk = signifikanteVedtak.isNotEmpty()
        val arenaSakIdListe = sorterVedtak(signifikanteVedtak).map { it.sakId }.distinct()

        return SignifikanteSakerResponse(harSignifikantHistorikk, arenaSakIdListe)
    }

    private fun rapporterMetrikkerForSignifikanteVedtak(vedtakene: List<no.nav.aap.arenaoppslag.kontrakt.intern.ArenaVedtak>) {
        vedtakene.forEach {
            Metrics.prometheus.rapporterMetrikkerForSignifikanteVedtak(it)
        }
    }
    internal fun sorterVedtak(vedtak: List<ArenaVedtak>): List<ArenaVedtak> {
        // Hvis saker uten tilDato finnes, sorter disse basert på db-order
        val utenSluttdato = vedtak.filter { it.tilDato == null }.reversed() // i reversed db-order (=nyeste først)
        // Hvis saker med tilDato finnes, sorter disse synkende på dato (=nyeste først)
        val medSluttdato = vedtak.filter { it.tilDato != null }.sortedByDescending { it.tilDato }
        return utenSluttdato + medSluttdato
    }

    // TODO vurder om vi skal cache en fnr->personId mapping her for å unngå gjentatte kall mot databasen
    fun personEksistererIAapArena(personidentifikatorer: List<String>): PersonEksistererIAAPArena {
        val personId = personRepository.hentPersonIdHvisEksisterer(personidentifikatorer.toSet())
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

    fun MeterRegistry.rapporterMetrikkerForSignifikanteVedtak(vedtak: ArenaVedtak): Counter {
        return this.counter(
            "arenaoppslag_signifikante_vedtak",
            listOf(
                Tag.of("type", vedtak.vedtaktypeKode),
                Tag.of("rettighet", vedtak.rettighetkode),
                Tag.of("status", vedtak.statusKode),
            )
        )
    }
}
