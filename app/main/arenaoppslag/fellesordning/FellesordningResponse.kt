package arenaoppslag.fellesordning

import java.time.LocalDate

data class FellesordningResponse(
    val personId: String,
    val tilDato: LocalDate,
    val fraDato: LocalDate,
)