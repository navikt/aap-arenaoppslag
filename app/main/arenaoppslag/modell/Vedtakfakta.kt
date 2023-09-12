package arenaoppslag.modell

import java.time.LocalDate

data class Vedtakfakta(
    val vedtakId: Int,
    val vedtakfaktakode: String,
    val vedtakverdi: String,
    val regDato: LocalDate,
    val regUser: String,
    val modDato: LocalDate,
    val modUser: String,
    val personId: Int,
)
