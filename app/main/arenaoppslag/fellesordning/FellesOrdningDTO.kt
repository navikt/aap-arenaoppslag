package arenaoppslag.fellesordning

import java.time.LocalDate

data class FellesOrdningDTO(
    val personId: String,
    val tilDato: LocalDate,
    val fraDato: LocalDate,
)