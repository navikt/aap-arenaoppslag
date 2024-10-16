package arenaoppslag.intern

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

data class SakStatus(
    val sakId: String,
    val vedtakStatusKode: String
)

data class PerioderMed11_17Response(
    val perioder: List<PeriodeMed11_17>
)

data class PeriodeMed11_17(
    val periode: Periode,
    val aktivitetsfaseKode: String,
    val aktivitetsfaseNavn: String
)