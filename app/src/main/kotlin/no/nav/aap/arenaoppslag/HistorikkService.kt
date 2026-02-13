package no.nav.aap.arenaoppslag

import no.nav.aap.arenaoppslag.database.HistorikkRepository
import no.nav.aap.arenaoppslag.database.PersonRepository
import no.nav.aap.arenaoppslag.kontrakt.intern.ArenaSak
import no.nav.aap.arenaoppslag.kontrakt.intern.NyereSakerResponse
import no.nav.aap.arenaoppslag.kontrakt.intern.Person
import no.nav.aap.arenaoppslag.kontrakt.intern.PersonEksistererIAAPArena
import no.nav.aap.arenaoppslag.kontrakt.intern.SignifikanteSakerResponse
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

        val relevanteArenaVedtak = historikkRepository.hentAlleSignifikanteVedtakForPerson(
            personId,
            virkningstidspunkt
        )

        val harSignifikantHistorikk = relevanteArenaVedtak.isNotEmpty()
        // TODO?
        // Filtrere relevanteArenaVedtak - grupper per sak, og dersom vi har en sak med kun AA115 i, ingen AAP-vedtak:
        // dropp den hvis den er eldre enn 6 måneder. Evt: dette gjøres av et eget endepunkt/felt i responsen.
        // Dette kompenserer for at de ikke er lukket i Arena selv om de bør være det.

        // TODO?
        // Filtrere relevanteArenaVedtak - dersom det kun finnes vedtak som utgår om opptil 3 mnd, inkl et AA115,
        // dropp dem, slik at listen blir tom. Evt: dette gjøres av et eget endepunkt/felt i responsen.
        // Vi bør da ta inn dato for start av den nye ytelsesperioden.
        val arenaSakIdListe = sorterSaker(relevanteArenaVedtak).map { it.sakId }.distinct()

        return SignifikanteSakerResponse(harSignifikantHistorikk, arenaSakIdListe)
    }

    internal fun sorterSaker(arenaSaker: List<ArenaSak>): List<ArenaSak> {
        // Hvis saker uten tilDato finnes, sorter disse basert på db-order
        val utenSluttdato = arenaSaker.filter { it.tilDato == null }.reversed() // i reversed db-order (=nyeste først)
        // Hvis saker med tilDato finnes, sorter disse synkende på dato (=nyeste først)
        val medSluttdato = arenaSaker.filter { it.tilDato != null }.sortedByDescending { it.tilDato }
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
        val nyereSaker = historikkRepository.hentIkkeAvbrutteSakerSisteFemÅrForPerson(personId)

        val harNyereHistorikk = nyereSaker.isNotEmpty()
        val arenaSakIdListe = sorterSaker(nyereSaker).map { it.sakId }.distinct()

        return NyereSakerResponse(harNyereHistorikk, arenaSakIdListe)
    }

}
