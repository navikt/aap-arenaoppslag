package no.nav.aap.arenaoppslag.service

import no.nav.aap.arenaoppslag.database.MeldekortRepository
import no.nav.aap.arenaoppslag.database.TelleverkRepository
import no.nav.aap.arenaoppslag.modeller.Meldekort
import no.nav.aap.arenaoppslag.modeller.PersonId
import no.nav.aap.arenaoppslag.modeller.ReduksjonRespons
import no.nav.aap.arenaoppslag.modeller.SakId
import no.nav.aap.arenaoppslag.modeller.TilkjentYtelseRad
import no.nav.aap.arenaoppslag.modeller.TilkjentYtelseResponse
import no.nav.aap.arenaoppslag.modeller.tilRespons
import kotlin.math.roundToInt

// Komposittjeneste som bygger den rike tilkjent-ytelse-visningen for en sak ved å bruke
// MeldekortRepository som byggekloss sammen med telleverk (gjenstående kvoter).
class TilkjentYtelserService(
    private val meldekortRepository: MeldekortRepository,
    private val telleverkRepository: TelleverkRepository,
) {

    fun hentTilkjenteYtelserForSak(sakId: SakId): TilkjentYtelseResponse {
        val data = meldekortRepository.hentForSak(sakId)
        val meldekortPerId = data.meldekort.associateBy { it.meldekortId }

        val personId = data.posteringer.firstOrNull()?.personId ?: data.meldekort.firstOrNull()?.personId
        val kvoter = if (personId != null) {
            telleverkRepository.hentTelleverkForPerson(PersonId(personId))
        } else {
            emptySet()
        }

        val rader = data.posteringer.map { postering ->
            val meldekort = postering.meldekortId?.let { meldekortPerId[it] }

            // Timer og reduksjon beregnes kun for meldekortlinjer — spesialutbetalinger har ingen meldekort.
            val timerArbeidetEtterStraff = meldekort?.let { timerArbeidetEtterStraffedager(it) }
            val reduksjon = meldekort?.let {
                byggReduksjon(it, timerArbeidetEtterStraff ?: 0.0, postering.dagsats, postering.dagsatsForSamordning, postering.insGrad)
            }

            TilkjentYtelseRad(
                fraOgMedDato = postering.periode.fraOgMedDato,
                tilOgMedDato = postering.periode.tilOgMedDato,
                uke = meldekort?.let { "${it.ukenrUke1}-${it.ukenrUke2}" },
                kilde = if (postering.meldekortId != null) KILDE_MELDEKORT else KILDE_SPESIALUTBETALING,
                dagsatsMedBarnetillegg = postering.dagsatsMedBarnetillegg,
                dagsats = postering.dagsats,
                beregnetBrutto = postering.belop,
                timerArbeidet = timerArbeidetEtterStraff,
                reduksjon = reduksjon,
                meldekort = meldekort?.tilRespons(),
            )
        }

        return TilkjentYtelseResponse(
            sakId = sakId.id,
            gjenstaaendeOrdinaerDager = kvoter.find { it.kode == KVOTE_ORDINAER }?.verdi,
            gjenstaaendeUnntakDager = kvoter.find { it.kode == KVOTE_UNNTAK }?.verdi,
            rader = rader,
        )
    }

    // Dager der forrige meldekort ble levert for sent ekskluderes fra grunnlaget: vi hopper over de
    // første `dagerForSent` dagene i perioden, og ignorerer timer arbeidet (og fravær) på disse dagene.
    private fun timerArbeidetEtterStraffedager(meldekort: Meldekort): Double {
        val aktivFraOgMed = meldekort.periode.fraOgMedDato.plusDays(meldekort.reduksjon.dagerForSent.toLong())
        return meldekort.dager
            .filter { !it.dato.isBefore(aktivFraOgMed) }
            .sumOf { it.timerArbeidet }
    }

    private fun byggReduksjon(
        meldekort: Meldekort,
        timerArbeidet: Double,
        dagsats: Int?,
        dagsatsForSamordning: Int?,
        insGrad: Int?,
    ): ReduksjonRespons {
        val dagerForSent = meldekort.reduksjon.dagerForSent
        // Straffedagene reduserer antall dager som inngår i fulltidsgrunnlaget.
        val aktiveDager = DAGER_I_MELDEKORTPERIODE - dagerForSent
        val timerArbeidetProsent = if (aktiveDager > 0) {
            (timerArbeidet / (aktiveDager * TIMER_PER_DAG) * 100).roundToInt()
        } else {
            0
        }
        val samordningsProsent = beregnSamordningsProsent(dagsats, dagsatsForSamordning)
        return ReduksjonRespons(
            levertForSentDager = dagerForSent,
            timerArbeidetProsent = timerArbeidetProsent,
            samordningsProsent = samordningsProsent,
            totalReduksjonProsent = timerArbeidetProsent + samordningsProsent,
            fravar = meldekort.reduksjon.fravar,
            sykedager = meldekort.reduksjon.sykedager,
            institusjonsProsent = insGrad,
        )
    }

    // Samordningsprosent = hvor mye dagsatsen er redusert fra før-samordning (DAGSFSAM) til etter (DAGS).
    private fun beregnSamordningsProsent(dagsats: Int?, dagsatsForSamordning: Int?): Int {
        if (dagsats == null || dagsatsForSamordning == null || dagsatsForSamordning == 0) return 0
        return ((dagsatsForSamordning - dagsats).toDouble() / dagsatsForSamordning * 100).roundToInt()
    }

    private companion object {
        private const val KILDE_MELDEKORT = "Meldekort"
        private const val KILDE_SPESIALUTBETALING = "Spesialutbetaling"
        // BEREGNINGSLEDD-koder for gjenstående kvote: AAP = ordinær periode, MAAPU = unntak §11-12.
        private const val KVOTE_ORDINAER = "AAP"
        private const val KVOTE_UNNTAK = "MAAPU"
        // Et meldekort dekker 14 dager, og en full arbeidsdag er 7,5 timer.
        private const val DAGER_I_MELDEKORTPERIODE = 14
        private const val TIMER_PER_DAG = 7.5
    }
}


