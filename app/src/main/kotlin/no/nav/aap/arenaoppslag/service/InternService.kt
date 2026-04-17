package no.nav.aap.arenaoppslag.service

import com.github.benmanes.caffeine.cache.Caffeine
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics
import java.time.Duration
import java.time.LocalDate
import no.nav.aap.arenaoppslag.Metrics.prometheus
import no.nav.aap.arenaoppslag.database.MaksimumRepository
import no.nav.aap.arenaoppslag.database.PeriodeRepository
import no.nav.aap.arenaoppslag.database.VedtakRepository
import no.nav.aap.arenaoppslag.kontrakt.intern.PerioderMed11_17Response
import no.nav.aap.arenaoppslag.kontrakt.intern.SakStatus
import no.nav.aap.arenaoppslag.kontrakt.intern.PerioderResponse
import no.nav.aap.arenaoppslag.kontrakt.modeller.Maksimum

class InternService(
    private val maksimumRepository: MaksimumRepository,
    private val periodeRepository: PeriodeRepository,
    private val vedtakRepository: VedtakRepository,
) {
    private val maksimumCache = Caffeine.newBuilder()
        .maximumSize(10_000)
        .expireAfterWrite(Duration.ofMinutes(15))
        .build<String, Maksimum>()

    private val sakerCache = Caffeine.newBuilder()
        .maximumSize(10_000)
        .expireAfterWrite(Duration.ofMinutes(15))
        .build<String, List<SakStatus>>()

    private val perioderCache = Caffeine.newBuilder()
        .maximumSize(10_000)
        .expireAfterWrite(Duration.ofMinutes(15))
        .build<String, PerioderResponse>()

    private val perioder11_17Cache = Caffeine.newBuilder()
        .maximumSize(10_000)
        .expireAfterWrite(Duration.ofMinutes(15))
        .build<String, PerioderMed11_17Response>()

    init {
        CaffeineCacheMetrics.monitor(prometheus, maksimumCache, "arenaoppslag_maksimum")
        CaffeineCacheMetrics.monitor(prometheus, sakerCache, "arenaoppslag_saker")
        CaffeineCacheMetrics.monitor(prometheus, perioderCache, "arenaoppslag_perioder")
        CaffeineCacheMetrics.monitor(prometheus, perioder11_17Cache, "arenaoppslag_perioder_11_17")
    }

    fun hentPerioder(fodselsnr: String, fraOgMedDato: LocalDate, tilOgMedDato: LocalDate): PerioderResponse =
        perioderCache.get("$fodselsnr-$fraOgMedDato-$tilOgMedDato") {
            val hentPerioder = periodeRepository.hentPerioder(fodselsnr, fraOgMedDato, tilOgMedDato)
            PerioderResponse(perioder = hentPerioder.map { it.tilKontrakt() })
        }

    fun hent11_17Perioder(
        fodselsnr: String, fraOgMedDato: LocalDate, tilOgMedDato: LocalDate
    ): PerioderMed11_17Response =
        perioder11_17Cache.get("$fodselsnr-$fraOgMedDato-$tilOgMedDato") {
            val perioder = periodeRepository.hentPeriodeInkludert11_17(fodselsnr, fraOgMedDato, tilOgMedDato)
            PerioderMed11_17Response(perioder = perioder.map { it.tilKontrakt() })
        }


    fun hentSaker(fodselsnummerene: Set<String>): List<SakStatus> {
        // Merk: kontraktobjektet heter fra gammelt av feilaktig SakStatus, selv om det omhandler VedtakStatus
        return fodselsnummerene.flatMap { fnr ->
            sakerCache.get(fnr) {
                vedtakRepository.hentVedtakStatuser(fnr)
                    .map { SakStatus(it.sakId, it.statusKode, it.periode, it.kilde) }
            }
        }
    }

    fun hentMaksimum(fodselsnr: String, fraOgMedDato: LocalDate, tilOgMedDato: LocalDate): Maksimum =
        maksimumCache.get("$fodselsnr-$fraOgMedDato-$tilOgMedDato") {
            maksimumRepository.hentMaksimumsløsning(
                fodselsnr, fraOgMedDato, tilOgMedDato
            ).tilKontrakt()
        }

}