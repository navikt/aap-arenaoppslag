package no.nav.aap.arenaoppslag.service

import com.github.benmanes.caffeine.cache.Caffeine
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics
import no.nav.aap.arenaoppslag.Metrics.prometheus
import no.nav.aap.arenaoppslag.Metrics.registrerAntallSignifikanteVedtak
import no.nav.aap.arenaoppslag.Metrics.registrerEnsligSignifikantVedtak
import no.nav.aap.arenaoppslag.Metrics.registrerNyesteSignifikanteVedtak
import no.nav.aap.arenaoppslag.Metrics.registrerSignifikantVedtak
import no.nav.aap.arenaoppslag.SakOgVedtakService
import no.nav.aap.arenaoppslag.database.HistorikkRepository
import no.nav.aap.arenaoppslag.kontrakt.apiv1.ArenaVedtak
import no.nav.aap.arenaoppslag.kontrakt.apiv1.HistorikkStatusDetaljResponse
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SignifikantHistorikkResponse
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SisteSakDetaljer
import no.nav.aap.arenaoppslag.kontrakt.apiv1.TelleverkForPerson
import no.nav.aap.arenaoppslag.modeller.PersonId
import java.time.LocalDate
import no.nav.aap.arenaoppslag.modeller.ArenaVedtak as ArenaVedtakModell

class HistorikkService(
    private val historikkRepository: HistorikkRepository,
    private val vedtakService: VedtakService,
    private val utbetalingService: PosteringService,
    private val telleverkService: TelleverkService,
    private val sakOgVedtakService: SakOgVedtakService
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
        val signifikanteVedtak: List<ArenaVedtakModell> = historikkRepository.hentAlleSignifikanteVedtakForPerson(
            personId,
            virkningstidspunkt
        )
        rapporterMetrikker(signifikanteVedtak)

        val harSignifikantHistorikk = signifikanteVedtak.isNotEmpty()

        return SignifikantHistorikkResponse(harSignifikantHistorikk, signifikanteVedtak.map {
            it.tilKontrakt()
        })
    }

    private fun rapporterMetrikker(vedtakene: List<ArenaVedtakModell>) {
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

    fun hentHistorikkStatusDetalj(personId: PersonId): HistorikkStatusDetaljResponse {
        val sisteVedtakMedMaksdato = vedtakService.hentSisteLøpendeVedtakDetalj(personId)
        val sisteSakMedVedtak = sisteVedtakMedMaksdato?.saksnummer()?.let { sakOgVedtakService.hentSakMedVedtak(it) }
        val vedtakISisteSak = sisteSakMedVedtak?.vedtak?.map {
            ArenaVedtak(
                sakId = sisteVedtakMedMaksdato.sakId,
                vedtaktypeKode = it.vedtaktypeKode,
                statusKode = it.statusKode,
                tilDato = it.tilDato,
                fraOgMed = it.fraOgMed,
                rettighetkode = it.rettighetkode,
                utfallkode = it.utfallkode
            )
        }
        val sisteSakDetaljer = sisteSakMedVedtak?.let {
            SisteSakDetaljer(
                sakId = it.sakId.toInt(),
                saknummer = "$it.aar-$it.lopenr",
                sakStatus = it.statuskode,
                sakRegistrert = it.registrertDato.toLocalDate(),
                sakAvsluttet = it.avsluttetDato?.toLocalDate(),
                unntaksvilkaarInnvilget = sisteVedtakMedMaksdato.unntaksvilkaarInnvilget,
                unntaksvilkaarGjelderFra = sisteVedtakMedMaksdato.unntaksvilkaarGjelderFra,
                sisteVedtak = sisteVedtakMedMaksdato.toKontrakt(),
                vedtakene = vedtakISisteSak ?: emptyList()
            )

        }

        val kvote = telleverkService.hentTelleverkForPerson(personId)?.let {
            TelleverkForPerson(
                // Korriger feilregistreringer i Arena-teller
                ordineerAAPKvote = if (it.ordineerAAPKvote < 0) 0 else it.ordineerAAPKvote,
                utvidetAAPKvote = if (sisteVedtakMedMaksdato?.unntaksvilkaarOppfylt() == true) {
                    it.utvidetAAPKvote
                } else null
            )
        }

        val sistePosteringsDato = utbetalingService.hentSisteAapUtbetalingForPerson(personId)

        return HistorikkStatusDetaljResponse(
            kvote,
            sisteSakDetaljer,
            sistePosteringsDato
        )
    }

}
