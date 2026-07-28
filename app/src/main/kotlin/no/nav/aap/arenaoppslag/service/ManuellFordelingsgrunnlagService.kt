package no.nav.aap.arenaoppslag.service

import no.nav.aap.arenaoppslag.kontrakt.apiv1.ManuellFordelingsgrunnlagResponse
import no.nav.aap.arenaoppslag.modeller.PersonId
import java.time.LocalDate

/**
 * Komposisjonstjeneste som setter sammen AAP-grunnlaget en saksbehandler trenger for å
 * vurdere om en søknad skal behandles i Arena eller Kelvin. Bruker eksisterende tjenester
 * som byggeklosser i stedet for å samle all logikk i én "god class".
 */
class ManuellFordelingsgrunnlagService(
    private val sakService: SakService,
    private val posteringService: PosteringService,
    private val telleverkService: TelleverkService,
) {
    fun hentManuellFordelingsgrunnlag(
        personId: PersonId,
        iDag: LocalDate = LocalDate.now(),
    ): ManuellFordelingsgrunnlagResponse? {
        // Uten en AAP-sak har grunnlaget ingen mening for vurderingen (Arena vs. Kelvin),
        // og telleverkstallene alene sier ingenting. Da returnerer vi null slik at ruten
        // kan svare 404 i stedet for et tomt 200-objekt.
        val sak = sakService.hentMaksdatoAapMedVedtakOgSak(personId) ?: return null

        val sisteUtbetaling = posteringService.hentSisteAapUtbetalingForPerson(personId)
        // Telleverket holder faktisk gjenstående kvote i dager, og er en mer presis kilde
        // enn å regne differansen mot maksdato.
        val telleverk = telleverkService.hentTelleverkForPerson(personId)

        return ManuellFordelingsgrunnlagResponse(
            saksnummer = sak.saknummer,
            erAktiv = sak.erLopende(),
            under52Uker = under52Uker(sak.sisteVedtak.til, iDag),
            gjenståendeOrdinæreDager = telleverk?.ordineerAAPKvote,
            // Samlet gjenstående unntaksperiode §11-12 (andre og tredje ledd).
            gjenståendeUnntaksDager = telleverk?.utvidetAAPKvote,
            sisteVedtak = sak.sisteVedtak,
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

