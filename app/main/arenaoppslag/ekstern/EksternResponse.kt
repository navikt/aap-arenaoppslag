package arenaoppslag.ekstern

import arenaoppslag.perioder.Periode
import java.time.LocalDate

data class VedtakResponse(
    val perioder: List<Periode>
)


data class VedtakFakta(
    var dagsmbt: Int,
    var barntill: Int,
    var dags: Int
)

