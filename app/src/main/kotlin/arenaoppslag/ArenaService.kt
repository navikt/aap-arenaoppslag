package arenaoppslag

import arenaoppslag.database.ArenaRepository
import no.nav.aap.arenaoppslag.kontrakt.intern.PerioderMed11_17Response
import no.nav.aap.arenaoppslag.kontrakt.intern.PersonEksistererIAAPArena
import no.nav.aap.arenaoppslag.kontrakt.intern.PersonHarSignifikantAAPArenaHistorikk
import no.nav.aap.arenaoppslag.kontrakt.intern.SakStatus
import no.nav.aap.arenaoppslag.kontrakt.intern.VedtakResponse
import no.nav.aap.arenaoppslag.kontrakt.modeller.Maksimum
import java.time.LocalDate

class ArenaService(private val arenaRepository: ArenaRepository) {
    fun hentRelevanteSakerForPerson(personIdentifikatorer: List<String>, virkningstidspunkt: LocalDate):
            PersonHarSignifikantAAPArenaHistorikk {
        val arenaData = arenaRepository.hentKanBehandlesIKelvin(personIdentifikatorer, virkningstidspunkt)

        return PersonHarSignifikantAAPArenaHistorikk(
            arenaData.kanBehandles, arenaData.arenaSakIdListe
        )
    }

    fun personEksistererIAapArena(personidentifikatorer: List<String>): PersonEksistererIAAPArena {
        return PersonEksistererIAAPArena(
            personidentifikatorer.map { personidentifikator ->
                arenaRepository.hentEksistererIAAPArena(personidentifikator)
            }.any { it }
        )

    }

    fun hentPerioder(personidentifikator: String, fraOgMedDato: LocalDate, tilOgMedDato: LocalDate):
            VedtakResponse {
        return VedtakResponse(
            perioder = arenaRepository.hentPerioder(
                personidentifikator,
                fraOgMedDato,
                tilOgMedDato
            ).map { it.tilKontrakt() }
        )
    }

    fun hent11_17Perioder(personidentifikator: String, fraOgMedDato: LocalDate, tilOgMedDato: LocalDate):
            PerioderMed11_17Response {
        return PerioderMed11_17Response(
            perioder = arenaRepository.hentPeriodeInkludert11_17(
                personidentifikator,
                fraOgMedDato,
                tilOgMedDato
            ).map { it.tilKontrakt() }
        )
    }

    fun hentSaker(personidentifikatorer: List<String>): List<SakStatus> {
        val saker = personidentifikatorer.flatMap { personidentifikator ->
            arenaRepository.hentSaker(personidentifikator)
        }
        return saker
    }

    fun hentMaksimum(personidentifikator: String, fraOgMedDato: LocalDate, tilOgMedDato: LocalDate): Maksimum {
        return arenaRepository.hentMaksimumsløsning(
            personidentifikator,
            fraOgMedDato,
            tilOgMedDato
        ).tilKontrakt()
    }

}
