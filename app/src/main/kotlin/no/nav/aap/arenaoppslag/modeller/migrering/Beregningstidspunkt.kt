package no.nav.aap.arenaoppslag.modeller.migrering

import no.nav.aap.arenaoppslag.kontrakt.migrering.ArenaBeregningstidspunktResponse
import no.nav.aap.arenaoppslag.modeller.ArenaVedtakfakta
import java.time.LocalDate

data class Beregningstidspunkt(val dato: LocalDate?) {
    fun tilKontrakt() = ArenaBeregningstidspunktResponse(beregningstidspunkt = dato)

    companion object {
        private const val BEREGNINGSDATO_KODE = "AAPBERDATO"

        fun fraVedtakfakta(vedtakfakta: List<ArenaVedtakfakta>) = Beregningstidspunkt(
            vedtakfakta.firstOrNull { it.kode == BEREGNINGSDATO_KODE }?.somDatoVerdi()
        )
    }
}
