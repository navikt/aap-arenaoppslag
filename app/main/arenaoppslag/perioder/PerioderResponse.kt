package arenaoppslag.perioder

import java.time.LocalDate

data class PerioderResponse(
    val perioder: List<Periode>
)

data class PerioderMed11_17Response(
    val perioder: List<PeriodeMed11_17>
)

data class Periode(
    val fraOgMedDato: LocalDate,
    val tilOgMedDato: LocalDate?
)

data class PeriodeMed11_17(
    val periode: Periode,
    val aktivitetsfaseKode: String,
    val aktivitetsfaseNavn: String
)
