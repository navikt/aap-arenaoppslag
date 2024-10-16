package arenaoppslag.ekstern

import java.time.LocalDate

data class Periode(
    val fraOgMedDato: LocalDate,
    val tilOgMedDato: LocalDate?
)

data class VedtakResponse(
    val perioder: List<Periode>
)


data class VedtakFakta(
    var dagsmbt: Int,
    var barntill: Int,
    var dags: Int
)

