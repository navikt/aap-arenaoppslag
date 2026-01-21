package arenaoppslag.aap

import arenaoppslag.aap.database.MaksimumRepository
import arenaoppslag.aap.database.PeriodeRepository
import arenaoppslag.aap.database.PersonRepository
import arenaoppslag.aap.database.SakRepository
import no.nav.aap.arenaoppslag.kontrakt.intern.ArenaSak
import no.nav.aap.arenaoppslag.kontrakt.intern.PerioderMed11_17Response
import no.nav.aap.arenaoppslag.kontrakt.intern.PersonEksistererIAAPArena
import no.nav.aap.arenaoppslag.kontrakt.intern.SignifikanteSakerResponse
import no.nav.aap.arenaoppslag.kontrakt.intern.Person
import no.nav.aap.arenaoppslag.kontrakt.intern.SakStatus
import no.nav.aap.arenaoppslag.kontrakt.intern.VedtakResponse
import no.nav.aap.arenaoppslag.kontrakt.modeller.Maksimum
import org.jetbrains.annotations.TestOnly
import java.time.LocalDate

class ArenaService(
    private val personRepository: PersonRepository,
    private val maksimumRepository: MaksimumRepository,
    private val periodeRepository: PeriodeRepository,
    private val sakRepository: SakRepository
) {

    @TestOnly
    fun hentAllePersoner(): List<Person> {
        return personRepository.hentAlle()
    }

    fun signifikanteSakerForPerson(
        personIdentifikatorer: List<String>, virkningstidspunkt: LocalDate
    ): SignifikanteSakerResponse {
        val relevanteArenaSaker = personRepository.hentAlleSignifikanteSakerForPerson(
                personIdentifikatorer,
                virkningstidspunkt
            )

        val harSignifikantHistorikk = relevanteArenaSaker.isNotEmpty()
        val arenaSakIdListe = sorterSaker(relevanteArenaSaker).map { it.sakId }.distinct()

        return SignifikanteSakerResponse(harSignifikantHistorikk, arenaSakIdListe)
    }

    internal fun sorterSaker(arenaSaker: List<ArenaSak>): List<ArenaSak> {
        // Hvis saker uten tilDato finnes, sorter disse basert på db-order
        val utenSluttdato = arenaSaker.filter { it.tilDato == null }.reversed() // i reversed db-order (=nyeste først)
        // Hvis saker med tilDato finnes, sorter disse synkende på dato (=nyeste først)
        val medSluttdato = arenaSaker.filter { it.tilDato != null }.sortedByDescending { it.tilDato }
        return utenSluttdato + medSluttdato
    }

    fun personEksistererIAapArena(personidentifikatorer: List<String>): PersonEksistererIAAPArena {
        return PersonEksistererIAAPArena(personidentifikatorer.map { personidentifikator ->
            personRepository.hentEksistererIAAPArena(personidentifikator)
        }.any { it })

    }

    fun hentPerioder(personidentifikator: String, fraOgMedDato: LocalDate, tilOgMedDato: LocalDate): VedtakResponse {
        val hentPerioder = periodeRepository.hentPerioder(
            personidentifikator, fraOgMedDato, tilOgMedDato
        )
        return VedtakResponse(perioder = hentPerioder.map { it.tilKontrakt() })
    }

    fun hent11_17Perioder(
        personidentifikator: String, fraOgMedDato: LocalDate, tilOgMedDato: LocalDate
    ): PerioderMed11_17Response {
        val perioder = periodeRepository.hentPeriodeInkludert11_17(
            personidentifikator, fraOgMedDato, tilOgMedDato
        )
        return PerioderMed11_17Response(perioder = perioder.map { it.tilKontrakt() })
    }

    fun hentSaker(personidentifikatorer: List<String>): List<SakStatus> {
        val saker = personidentifikatorer.flatMap { personidentifikator ->
            sakRepository.hentSakStatuser(personidentifikator)
        }
        return saker
    }

    fun hentMaksimum(personidentifikator: String, fraOgMedDato: LocalDate, tilOgMedDato: LocalDate): Maksimum {
        return maksimumRepository.hentMaksimumsløsning(
            personidentifikator, fraOgMedDato, tilOgMedDato
        ).tilKontrakt()
    }

}
