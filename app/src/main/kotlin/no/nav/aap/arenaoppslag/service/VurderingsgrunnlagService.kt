package no.nav.aap.arenaoppslag.service

import no.nav.aap.arenaoppslag.kontrakt.apiv1.VurderingsgrunnlagResponse
import no.nav.aap.arenaoppslag.modeller.PersonId
import java.time.LocalDate

/**
 * Komposisjonstjeneste som setter sammen AAP-grunnlaget en saksbehandler trenger for å
 * vurdere om en søknad skal behandles i Arena eller Kelvin. Bruker eksisterende tjenester
 * som byggeklosser i stedet for å samle all logikk i én "god class".
 */
class VurderingsgrunnlagService(
    private val sakService: SakService,
    private val posteringService: PosteringService,
    private val telleverkService: TelleverkService,
) {
    fun hentVurderingsgrunnlag(
        personId: PersonId,
        iDag: LocalDate = LocalDate.now(),
    ): VurderingsgrunnlagResponse {
        val sak = sakService.hentMaksdatoAapMedVedtakOgSak(personId)
        val sisteUtbetaling = posteringService.hentSisteAapUtbetalingForPerson(personId)
        // Telleverket holder faktisk gjenstående kvote i dager, og er en mer presis kilde
        // enn å regne differansen mot maksdato.
        val telleverk = telleverkService.hentTelleverkForPerson(personId)

        return VurderingsgrunnlagResponse(
            saksnummer = sak?.saknummer,
            erAktiv = sak?.erLopende() ?: false,
            under52Uker = sak?.let { under52Uker(it.sisteVedtak.til, iDag) },
            gjenstaaendeOrdinaerDager = telleverk?.ordineerAAPKvote,
            // Samlet gjenstående unntaksperiode §11-12 (andre og tredje ledd).
            gjenstaaendeUnntakDager = telleverk?.utvidetAAPKvote,
            sisteVedtak = sak?.sisteVedtak,
            sisteUtbetaling = sisteUtbetaling,
        )
    }

    /**
     * Samsvarer med visningsklienten som måler 52 uker fra siste vedtaks til-dato (TDATO).
     * Et løpende vedtak (til-dato = null) regnes som innenfor 52 uker.
     */
    private fun under52Uker(sisteVedtakTil: LocalDate?, iDag: LocalDate): Boolean {
        if (sisteVedtakTil == null) return true
        return !sisteVedtakTil.isBefore(iDag.minusWeeks(UKER_GRENSE))
    }


    companion object {
        private const val UKER_GRENSE = 52L
    }
}

