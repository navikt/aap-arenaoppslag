package arenaoppslag.dsop

data class DsopRequest(
    val personId: String,
    val periode: Periode,
    val samtykkePeriode: Periode
)
