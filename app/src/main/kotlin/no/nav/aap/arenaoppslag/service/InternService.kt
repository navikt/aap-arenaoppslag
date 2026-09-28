package no.nav.aap.arenaoppslag.service

import com.github.benmanes.caffeine.cache.Caffeine
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics
import kotlinx.coroutines.CoroutineDispatcher
import no.nav.aap.arenaoppslag.Metrics.prometheus
import no.nav.aap.arenaoppslag.database.MaksimumRepository
import no.nav.aap.arenaoppslag.database.PeriodeRepository
import no.nav.aap.arenaoppslag.database.VedtakRepository
import no.nav.aap.arenaoppslag.database.målDbKall
import no.nav.aap.arenaoppslag.kontrakt.intern.PerioderMed11_17Response
import no.nav.aap.arenaoppslag.kontrakt.intern.PerioderResponse
import no.nav.aap.arenaoppslag.kontrakt.intern.SakStatus
import no.nav.aap.arenaoppslag.kontrakt.modeller.Maksimum
import java.time.Duration
import java.time.LocalDate

@Suppress("MagicNumber")
class InternService(
    private val maksimumRepository: MaksimumRepository,
    private val periodeRepository: PeriodeRepository,
    private val vedtakRepository: VedtakRepository,
    // Blokkerende JDBC-kall må avlastes fra Ktor/Netty sine event loop-tråder (se AppConfig.ktorParallellitet),
    // ellers vil ett tregt Oracle-kall blokkere HELE applikasjonen for alle andre samtidige kall. Se
    // ArenaDatasource.tilDbDispatcher() for hvordan denne dimensjoneres etter HikariCP sin maximumPoolSize.
    private val dbDispatcher: CoroutineDispatcher,
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

    suspend fun hentPerioder(fodselsnr: String, fraOgMedDato: LocalDate, tilOgMedDato: LocalDate): PerioderResponse =
        dbDispatcher.målDbKall("perioder") {
            perioderCache.get("$fodselsnr-$fraOgMedDato-$tilOgMedDato") {
                val hentPerioder = periodeRepository.hentPerioder(fodselsnr, fraOgMedDato, tilOgMedDato)
                PerioderResponse(perioder = hentPerioder.map { it.tilKontrakt() })
            }
        }

    suspend fun hent11_17Perioder(
        fodselsnr: String, fraOgMedDato: LocalDate, tilOgMedDato: LocalDate
    ): PerioderMed11_17Response =
        dbDispatcher.målDbKall("perioder_11_17") {
            perioder11_17Cache.get("$fodselsnr-$fraOgMedDato-$tilOgMedDato") {
                val perioder = periodeRepository.hentPeriodeInkludert11_17(fodselsnr, fraOgMedDato, tilOgMedDato)
                PerioderMed11_17Response(perioder = perioder.map { it.tilKontrakt() })
            }
        }


    suspend fun hentSaker(fodselsnummerene: Set<String>): List<SakStatus> = dbDispatcher.målDbKall("saker") {
        // Merk: kontraktobjektet heter fra gammelt av feilaktig SakStatus, selv om det omhandler VedtakStatus
        fodselsnummerene.flatMap { fnr ->
            sakerCache.get(fnr) {
                vedtakRepository.hentVedtakStatuser(fnr)
                    .map { SakStatus(it.sakId, it.statusKode, it.periode.tilKontrakt(), it.kilde) }
            }
        }
    }

    suspend fun hentMaksimum(fodselsnr: String, fraOgMedDato: LocalDate, tilOgMedDato: LocalDate): Maksimum =
        dbDispatcher.målDbKall("maksimum") {
            maksimumCache.get("$fodselsnr-$fraOgMedDato-$tilOgMedDato") {
                maksimumRepository.hentMaksimumsløsning(
                    fodselsnr, fraOgMedDato, tilOgMedDato
                ).tilKontrakt()
            }
        }

}
