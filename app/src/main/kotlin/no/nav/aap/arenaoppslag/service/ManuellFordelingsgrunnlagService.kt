package no.nav.aap.arenaoppslag.service

import no.nav.aap.arenaoppslag.kontrakt.intern.ManuellFordelingsgrunnlagResponse
import no.nav.aap.arenaoppslag.modeller.PersonId
import java.time.LocalDate


class ManuellFordelingsgrunnlagService(
    private val sakService: SakService,
    private val posteringService: PosteringService,
    private val telleverkService: TelleverkService,
    private val oppgaveService: OppgaveService,
) {
    fun hentManuellFordelingsgrunnlag(
        personId: PersonId,
        iDag: LocalDate = LocalDate.now(),
    ): ManuellFordelingsgrunnlagResponse? {
        val sak = sakService.hentMaksdatoAapMedVedtakOgSak(personId) ?: return null

        val sisteUtbetaling = posteringService.hentSisteAapUtbetalingForPerson(personId)
        val telleverk = telleverkService.hentTelleverkForPerson(personId)
        val oppgaver = oppgaveService.hentOppgaverForPerson(personId)

        return ManuellFordelingsgrunnlagResponse(
            saksnummer = sak.saknummer,
            erAktiv = sak.erLopende(),
            under52Uker = under52Uker(sak.sisteVedtak.til, iDag),
            gjenståendeOrdinæreDager = telleverk?.ordineerAAPKvote,
            gjenståendeUnntaksDager = telleverk?.utvidetAAPKvote,
            sisteVedtak = sak.sisteVedtak,
            sisteUtbetaling = sisteUtbetaling,
            oppgaver = oppgaver.map { it.tilKontrakt() },
        )
    }


    private fun under52Uker(sisteVedtakTil: LocalDate?, iDag: LocalDate): Boolean {
        if (sisteVedtakTil == null) return true
        return !sisteVedtakTil.isBefore(iDag.minusWeeks(UKER_GRENSE))
    }


    companion object {
        private const val UKER_GRENSE = 52L
    }
}

