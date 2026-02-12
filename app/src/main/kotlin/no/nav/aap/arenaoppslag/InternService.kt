package no.nav.aap.arenaoppslag

import no.nav.aap.arenaoppslag.database.MaksimumRepository
import no.nav.aap.arenaoppslag.database.PeriodeRepository
import no.nav.aap.arenaoppslag.database.SakRepository
import no.nav.aap.arenaoppslag.kontrakt.intern.PerioderMed11_17Response
import no.nav.aap.arenaoppslag.kontrakt.intern.SakStatus
import no.nav.aap.arenaoppslag.kontrakt.intern.VedtakResponse
import no.nav.aap.arenaoppslag.kontrakt.modeller.Maksimum
import java.time.LocalDate

class InternService(
    private val maksimumRepository: MaksimumRepository,
    private val periodeRepository: PeriodeRepository,
    private val sakRepository: SakRepository
) {

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
