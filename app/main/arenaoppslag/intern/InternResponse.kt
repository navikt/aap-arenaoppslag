package arenaoppslag.intern

import arenaoppslag.perioder.Periode

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
