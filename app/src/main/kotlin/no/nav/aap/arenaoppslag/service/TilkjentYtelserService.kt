package no.nav.aap.arenaoppslag.service

import no.nav.aap.arenaoppslag.database.MeldekortRepository
import no.nav.aap.arenaoppslag.database.TelleverkRepository
import no.nav.aap.arenaoppslag.modeller.PersonId
import no.nav.aap.arenaoppslag.modeller.SakId
import no.nav.aap.arenaoppslag.modeller.TilkjentYtelseRad
import no.nav.aap.arenaoppslag.modeller.TilkjentYtelseResponse
import no.nav.aap.arenaoppslag.modeller.tilReduksjonRespons
import no.nav.aap.arenaoppslag.modeller.tilRespons

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
            TilkjentYtelseRad(
                fraOgMedDato = postering.periode.fraOgMedDato,
                tilOgMedDato = postering.periode.tilOgMedDato,
                uke = meldekort?.let { "${it.ukenrUke1}-${it.ukenrUke2}" },
                kilde = if (postering.meldekortId != null) KILDE_MELDEKORT else KILDE_SPESIALUTBETALING,
                dagsatsMedBarnetillegg = postering.dagsatsMedBarnetillegg,
                dagsats = postering.dagsats,
                beregnetBrutto = postering.belop,
                timerArbeidet = meldekort?.dager?.sumOf { it.timerArbeidet },
                reduksjon = meldekort?.reduksjon?.tilReduksjonRespons(),
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

    private companion object {
        private const val KILDE_MELDEKORT = "Meldekort"
        private const val KILDE_SPESIALUTBETALING = "Spesialutbetaling"
        // BEREGNINGSLEDD-koder for gjenstående kvote: AAP = ordinær periode, MAAPU = unntak §11-12.
        private const val KVOTE_ORDINAER = "AAP"
        private const val KVOTE_UNNTAK = "MAAPU"
    }
}


