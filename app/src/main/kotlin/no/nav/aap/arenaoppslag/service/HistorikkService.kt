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
import no.nav.aap.arenaoppslag.modeller.ArenaVedtakMedDetaljer
import no.nav.aap.arenaoppslag.modeller.ArenaVedtakMedMaxdato
import no.nav.aap.arenaoppslag.modeller.PersonId
import org.slf4j.LoggerFactory
import java.time.LocalDate
import no.nav.aap.arenaoppslag.modeller.ArenaVedtak as ArenaVedtakModell

class HistorikkService(
    private val historikkRepository: HistorikkRepository,
    private val vedtakService: VedtakService,
    private val utbetalingService: PosteringService,
    private val telleverkService: TelleverkService,
    private val sakOgVedtakService: SakOgVedtakService,
    private val vedtakfaktaService: VedtakfaktaService,
) {
    private val logger = LoggerFactory.getLogger(HistorikkService::class.java)

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
                utfallkode = it.utfallkode,
                vedtakId = it.vedtakId,
                aktivitetsfaseKode = it.aktivitetsfaseKode,
            )
        }

        val (unntaksvilkaarInnvilget, unntaksvilkaarGjelderFra) = hent11_12StatusForSaken(
            sisteVedtakMedMaksdato,
            sisteSakMedVedtak?.vedtak
        )

        val sisteSakDetaljer = sisteSakMedVedtak?.let {
            SisteSakDetaljer(
                sakId = it.sakId.toInt(),
                saknummer = "$it.aar-$it.lopenr",
                sakStatus = it.statuskode,
                sakRegistrert = it.registrertDato.toLocalDate(),
                sakAvsluttet = it.avsluttetDato?.toLocalDate(),
                unntaksvilkaarInnvilget = unntaksvilkaarInnvilget,
                unntaksvilkaarGjelderFra = unntaksvilkaarGjelderFra,
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

    private fun hent11_12StatusForSaken(
        sisteLøpendeVedtak: ArenaVedtakMedMaxdato?,
        vedtakeneISisteSak: List<ArenaVedtakMedDetaljer>?
    ): Pair<Boolean?, LocalDate?> {
        // Hvor dataene ligger avhenger av om 11-12 vurderingen er gjort etter gammel eller ny prosedyre.

        // Er 11-12 vurderingen registrert etter ny prosedyre?
        val eget11_12vedtak = vedtakeneISisteSak?.filter {
            it.fraOgMed?.isAfter(sisteLøpendeVedtak?.tilDato) == true
                    && it.vedtaktypeKode == "O"
                    && it.rettighetkode == "AAP"
                    && it.utfallkode == "NEI"
                    && it.aktivitetsfaseKode in listOf("AU", "UA")
        }?.sortedByDescending { it.fraOgMed }

        if (eget11_12vedtak != null && eget11_12vedtak.size > 1) {
            logger.warn(
                "Fant {} egne 11-12-vedtak i siste sak. Bruker vedtaket med nyeste fra-og-med-dato.",
                eget11_12vedtak.size
            )
        }

        val faktaPåEget11_12Vedtak = eget11_12vedtak?.firstOrNull()?.let {
            val faktaene = vedtakfaktaService.hentForVedtakliste(listOf(it.vedtakId))
            val fakta = faktaene.getOrDefault(it.vedtakId, emptyList())
            val unntakInnvilget = fakta.firstOrNull { it.kode == "UNNTAKAAP" }?.somBooleanVerdi()
            val unntaksdato = fakta.firstOrNull { it.kode == "AAPVILKUNN" }?.somDatoVerdi()
            Pair(unntakInnvilget, unntaksdato)
        }

        // Er 11-12 vurderingen registrert etter gammel prosedyre?
        val faktaPåLøpendeVedtak = sisteLøpendeVedtak?.unntaksvilkaarInnvilget?.let {
            Pair(
                sisteLøpendeVedtak.unntaksvilkaarInnvilget,
                sisteLøpendeVedtak.unntaksvilkaarGjelderFra
            )
        }

        return when {
            faktaPåLøpendeVedtak != null -> faktaPåLøpendeVedtak
            faktaPåEget11_12Vedtak != null -> faktaPåEget11_12Vedtak
            else -> Pair(null, null)
        }
    }

}
