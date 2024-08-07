package arenaoppslag.fellesordningen

import java.time.LocalDate

data class VedtakResponse(
    val perioder: List<VedtakPeriode>
)

data class VedtakPeriode(
    val fraOgMedDato: LocalDate,
    val tilOgMedDato: LocalDate?
)

data class VedtakFakta(
    var dagsmbt: Int,
    var barntill: Int,
    var dags: Int
)