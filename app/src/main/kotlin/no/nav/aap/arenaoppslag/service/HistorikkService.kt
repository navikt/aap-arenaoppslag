package no.nav.aap.arenaoppslag.service

import com.github.benmanes.caffeine.cache.Caffeine
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics
import no.nav.aap.arenaoppslag.Metrics.prometheus
import no.nav.aap.arenaoppslag.Metrics.registrerAntallSignifikanteVedtak
import no.nav.aap.arenaoppslag.Metrics.registrerEnsligSignifikantVedtak
import no.nav.aap.arenaoppslag.Metrics.registrerNyesteSignifikanteVedtak
import no.nav.aap.arenaoppslag.Metrics.registrerSignifikantVedtak
import no.nav.aap.arenaoppslag.database.HistorikkRepository
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SignifikantHistorikkResponse
import no.nav.aap.arenaoppslag.kontrakt.apiv1.TelleverkForPerson
import no.nav.aap.arenaoppslag.modeller.ArenaVedtak
import no.nav.aap.arenaoppslag.modeller.HistorikkStatusDetaljer
import no.nav.aap.arenaoppslag.modeller.PersonId
import java.time.LocalDate

class HistorikkService(
    private val historikkRepository: HistorikkRepository,
    private val vedtakService: VedtakService,
    private val utbetalingService: PosteringService,
    private val telleverkService: TelleverkService
) {

    // Lagrer mappingen fødselsnr -> arena-personId. Bare treff i databasen lagres.
    @Suppress("MagicNumber")
    private val personIdCache = Caffeine.newBuilder()
        .maximumSize(30_000)
        .recordStats()
        .build<String, Int>()

    init {
        CaffeineCacheMetrics.monitor(prometheus, personIdCache, "arenaoppslag_person_id")
    }

    fun signifikantHistorikk(
        personId: PersonId,
        virkningstidspunkt: LocalDate
    ): SignifikantHistorikkResponse {
        val signifikanteVedtak = historikkRepository.hentAlleSignifikanteVedtakForPerson(
            personId,
            virkningstidspunkt
        )
        rapporterMetrikker(signifikanteVedtak)

        val harSignifikantHistorikk = signifikanteVedtak.isNotEmpty()

        return SignifikantHistorikkResponse(harSignifikantHistorikk, signifikanteVedtak.map {
            it.tilKontrakt()
        })
    }

    private fun rapporterMetrikker(vedtakene: List<ArenaVedtak>) {
        if (vedtakene.isEmpty()) return

        vedtakene.forEach {
            prometheus.registrerSignifikantVedtak(it)
        }

        prometheus.registrerNyesteSignifikanteVedtak(vedtakene.first())

        prometheus.registrerAntallSignifikanteVedtak(vedtakene.size)

        if (vedtakene.size == 1) {
            prometheus.registrerEnsligSignifikantVedtak(vedtakene.first())
        }
    }

    fun hentHistorikkStatusDetalj(personId: PersonId): HistorikkStatusDetaljer {
        val sakMedSisteVedtakOgMaksdato = vedtakService.hentSisteLøpendeVedtakDetalj(personId)

        val kvoteTall = telleverkService.hentTelleverkForPerson(personId)
        val kvote = kvoteTall?.let {
            TelleverkForPerson(
                // Korriger feilregistreringer i Arena-teller
                ordineerAAPKvote = if (it.ordineerAAPKvote < 0) 0 else it.ordineerAAPKvote,
                utvidetAAPKvote = if (sakMedSisteVedtakOgMaksdato?.unntaksvilkaarOppfylt() == true) {
                    it.utvidetAAPKvote
                } else null
            )
        }

        val sistePosteringsDato = utbetalingService.hentSisteAapUtbetalingForPerson(personId)


        return HistorikkStatusDetaljer(kvote, sakMedSisteVedtakOgMaksdato, sistePosteringsDato)
    }

}
