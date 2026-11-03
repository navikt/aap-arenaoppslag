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
import no.nav.aap.arenaoppslag.kontrakt.apiv1.HistorikkStatusDetaljResponse
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SignifikantHistorikkResponse
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

        val sisteSakDetaljer = if (sisteVedtakMedMaksdato != null && sisteSakMedVedtak != null) {
            val (unntaksvilkaarInnvilget, unntaksvilkaarGjelderFra) = hent11_12StatusForSaken(
                sisteVedtakMedMaksdato,
                sisteSakMedVedtak.vedtak,
                sisteVedtakMedMaksdato.sakId // samme sakId på alle vedtakene
            )

            sisteSakMedVedtak.tilSisteSakDetaljer(
                sisteVedtak = sisteVedtakMedMaksdato,
                unntaksvilkaarInnvilget = unntaksvilkaarInnvilget,
                unntaksvilkaarGjelderFra = unntaksvilkaarGjelderFra,
            )
        } else null

        val kvote = telleverkService.hentTelleverkForPerson(personId)?.let {
            TelleverkForPerson(
                // Korriger feilregistreringer i Arena-teller
                ordineerAAPKvote = if (it.ordineerAAPKvote < 0) 0 else it.ordineerAAPKvote,
                utvidetAAPKvote = if (it.utvidetAAPKvote != null && it.utvidetAAPKvote < 0) 0 else it.utvidetAAPKvote
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
        vedtakeneISisteSak: List<ArenaVedtakMedDetaljer>?,
        sakId: Int?
    ): Pair<Boolean?, LocalDate?> {
        if (vedtakeneISisteSak.isNullOrEmpty()) {
            logger.info("Fant ingen vedtak for sakId=$sakId, kan ikke hente 11-12 status")
            return Pair(null, null)
        }
        // Hvor dataene ligger avhenger av om 11-12 vurderingen er gjort etter gammel eller ny prosedyre.

        // Er 11-12 vurderingen registrert etter ny prosedyre?
        val eget11_12vedtak = vedtakeneISisteSak.filter {
            it.fraOgMed?.isAfter(sisteLøpendeVedtak?.tilDato) == true
                    && it.vedtaktypeKode == "O"
                    && it.rettighetkode == "AAP"
                    && it.utfallkode == "NEI"
                    && it.aktivitetsfaseKode in listOf("AU", "UA")
        }
        val faktaPåEget11_12Vedtak = hentUnntakFraVedtakfakta(eget11_12vedtak, sakId)

        // Er 11-12 vurderingen registrert på et Stans-vedtak?
        val stansvedtakMed11_12 = vedtakeneISisteSak.filter {
            it.fraOgMed?.isAfter(sisteLøpendeVedtak?.tilDato) == true
                    && it.vedtaktypeKode == "S"
                    && it.rettighetkode == "AAP"
                    && it.utfallkode == "JA"
                    && it.aktivitetsfaseKode in listOf("AU", "UA")
        }
        val faktaPåStanset11_12Vedtak = hentUnntakFraVedtakfakta(stansvedtakMed11_12, sakId)

        // Er 11-12 vurderingen registrert etter gammel prosedyre?
        val faktaPåLøpendeVedtak = sisteLøpendeVedtak?.unntaksvilkaarInnvilget?.let {
            Pair(
                sisteLøpendeVedtak.unntaksvilkaarInnvilget,
                sisteLøpendeVedtak.unntaksvilkaarGjelderFra
            )
        }

        val resultat = when {
            faktaPåLøpendeVedtak != null -> faktaPåLøpendeVedtak
            faktaPåEget11_12Vedtak != null -> faktaPåEget11_12Vedtak
            faktaPåStanset11_12Vedtak != null -> faktaPåStanset11_12Vedtak
            else -> Pair(null, null) // ingen vurdering registrert
        }

        return resultat
    }

    private fun hentUnntakFraVedtakfakta(
        aktulleVedtak: List<ArenaVedtakMedDetaljer>,
        sakId: Int?
    ): Pair<Boolean, LocalDate>? {
        val vedtakene = aktulleVedtak.sortedByDescending { it.fraOgMed }.map { it.vedtakId }
        val faktaPerVedtak = vedtakfaktaService.hentForVedtakliste(vedtakene)
        val faktaene = vedtakene.mapNotNull { vedtakId -> /* må ha en ikke-null mappet verdi for å være relevant */
            val fakta = faktaPerVedtak[vedtakId].orEmpty()
            val unntakInnvilget = fakta.firstOrNull { it.kode == "UNNTAKAAP" }?.somBooleanVerdi()
            val unntaksdato = fakta.firstOrNull { it.kode == "AAPVILKUNN" }?.somDatoVerdi()
            if (unntakInnvilget != null && unntaksdato != null) {
                Pair(unntakInnvilget, unntaksdato)
            } else {
                if (unntakInnvilget != null || unntaksdato != null) {
                    logger.warn(
                        "Fant kun en av vedtakfakta-verdiene for 11-12 for vedtakId=${vedtakId}. " +
                                "Dette kan være en feil i Arena."
                    )
                }
                null
            }
        }

        if (vedtakene.isNotEmpty() && faktaene.isEmpty()) {
            logger.warn(
                "Fant relevante vedtak for 11-12 for sak {} , men ingen vedtakfakta. Dette kan være en feil i Arena.",
                sakId
            )
        }

        if (faktaene.size > 1) {
            logger.warn("Fant flere vedtakfakta for 11-12 for sak {}. Bruker den første.", sakId)
        }

        return faktaene.firstOrNull() // de er sortert etter fraOgMed, så første er den nyeste
    }

}
