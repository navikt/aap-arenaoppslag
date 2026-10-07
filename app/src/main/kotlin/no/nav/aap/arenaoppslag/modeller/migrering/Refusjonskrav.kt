package no.nav.aap.arenaoppslag.modeller.migrering

import no.nav.aap.arenaoppslag.kontrakt.migrering.ArenaRefusjonskrav
import no.nav.aap.arenaoppslag.kontrakt.migrering.ArenaRefusjonskravResponse
import no.nav.aap.arenaoppslag.modeller.ArenaVedtakfakta
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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

        private val DATO_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")

        fun fraVedtakfakta(vedtakfakta: List<ArenaVedtakfakta>): Refusjonskrav? {
            val aarsak = verdiFor(vedtakfakta, AARSAK_KODE) ?: return null

            return Refusjonskrav(
                aarsak = aarsak,
                fraDato = datoFor(vedtakfakta, FRA_DATO_KODE),
                tilDato = datoFor(vedtakfakta, TIL_DATO_KODE),
            )
        }

        private fun verdiFor(vedtakfakta: List<ArenaVedtakfakta>, kode: String): String? =
            vedtakfakta.firstOrNull { it.kode == kode }?.verdi?.takeIf { it.isNotBlank() }

        private fun datoFor(vedtakfakta: List<ArenaVedtakfakta>, kode: String): LocalDate? {
            val verdi = verdiFor(vedtakfakta, kode) ?: return null
            return LocalDate.parse(verdi, DATO_FORMAT)
        }
    }
}
