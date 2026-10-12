package no.nav.aap.arenaoppslag.modeller.migrering

import no.nav.aap.arenaoppslag.kontrakt.migrering.ArenaRefusjonskrav
import no.nav.aap.arenaoppslag.kontrakt.migrering.ArenaRefusjonskravResponse
import no.nav.aap.arenaoppslag.modeller.ArenaVedtakfakta
import java.time.LocalDate

data class RefusjonskravForSak(val refusjonskrav: Refusjonskrav?) {
    fun tilKontrakt() = ArenaRefusjonskravResponse(refusjonskrav = refusjonskrav?.tilKontrakt())
}

data class Refusjonskrav(
    val aarsak: String,
    val fraDato: LocalDate?,
    val tilDato: LocalDate?,
) {
    fun tilKontrakt() = ArenaRefusjonskrav(
        aarsak = aarsak,
        fraDato = fraDato,
        tilDato = tilDato,
    )

    companion object {
        private const val AARSAK_KODE = "UTBETVENTK"
        private const val FRA_DATO_KODE = "UTBETVENTF"
        private const val TIL_DATO_KODE = "UTBETVENTT"

        fun fraVedtakfakta(vedtakfakta: List<ArenaVedtakfakta>): Refusjonskrav? {
            val aarsak = vedtakfakta.firstOrNull { it.kode == AARSAK_KODE }?.somIkkeTomVerdi() ?: return null

            return Refusjonskrav(
                aarsak = aarsak,
                fraDato = vedtakfakta.firstOrNull { it.kode == FRA_DATO_KODE }?.somDatoVerdi(),
                tilDato = vedtakfakta.firstOrNull { it.kode == TIL_DATO_KODE }?.somDatoVerdi(),
            )
        }

    }
}
