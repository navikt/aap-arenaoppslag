package arenaoppslag.fellesordning

import java.time.LocalDate

data class FellesordningRespone(
    val personId: String,
    val tilDato: LocalDate,
    val fraDato: LocalDate,
)