package arenaoppslag.fellesordningen

import java.time.LocalDate

data class VedtakResponse(
    val personId: String,
    val perioder: List<VedtakPeriode>
)

data class VedtakPeriode(
    val fraDato: LocalDate,
    val tilDato: LocalDate?
)
