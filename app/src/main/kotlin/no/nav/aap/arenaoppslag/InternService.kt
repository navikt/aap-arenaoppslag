package no.nav.aap.arenaoppslag

import no.nav.aap.arenaoppslag.database.MaksimumRepository
import no.nav.aap.arenaoppslag.database.PeriodeRepository
import no.nav.aap.arenaoppslag.database.VedtakRepository
import no.nav.aap.arenaoppslag.kontrakt.intern.PerioderMed11_17Response
import no.nav.aap.arenaoppslag.kontrakt.intern.SakStatus
import no.nav.aap.arenaoppslag.kontrakt.intern.VedtakResponse
import no.nav.aap.arenaoppslag.kontrakt.modeller.Maksimum
import java.time.LocalDate

class InternService(
    private val maksimumRepository: MaksimumRepository,
    private val periodeRepository: PeriodeRepository,
    private val vedtakRepository: VedtakRepository
) {

    fun hentPerioder(fodselsnr: String, fraOgMedDato: LocalDate, tilOgMedDato: LocalDate): VedtakResponse {
        val hentPerioder = periodeRepository.hentPerioder(
            fodselsnr, fraOgMedDato, tilOgMedDato
        )
        return VedtakResponse(perioder = hentPerioder.map { it.tilKontrakt() })
    }

    fun hent11_17Perioder(
        fodselsnr: String, fraOgMedDato: LocalDate, tilOgMedDato: LocalDate
    ): PerioderMed11_17Response {
        val perioder = periodeRepository.hentPeriodeInkludert11_17(
            fodselsnr, fraOgMedDato, tilOgMedDato
        )
        return PerioderMed11_17Response(perioder = perioder.map { it.tilKontrakt() })
    }

    fun hentSaker(fodselsnummerene: Set<String>): List<SakStatus> {
        val vedtak = fodselsnummerene.flatMap { fnr ->
            vedtakRepository.hentVedtakStatuser(fnr)
        }
        // Merk: kontraktobjektet heter fra gammelt av feilaktig SakStatus, selv om det omhandler VedtakStatus
        return vedtak.map { SakStatus(it.sakId, it.statusKode, it.periode, it.kilde) }
    }

    fun hentMaksimum(fodselsnr: String, fraOgMedDato: LocalDate, tilOgMedDato: LocalDate): Maksimum {
        return maksimumRepository.hentMaksimumsløsning(
            fodselsnr, fraOgMedDato, tilOgMedDato
        ).tilKontrakt()
    }

}
