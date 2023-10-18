package arenaoppslag.fellesordning

import java.time.LocalDate

data class FellesordningResponse(val personId: String, val perioder: List<VedtakPeriode>)

data class VedtakPeriode(val fraDato:LocalDate, val tilDato:LocalDate)