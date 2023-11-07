package arenaoppslag.dsop

data class MeldekortResponse(
    val uttrekksperiode: Periode,
    val meldekortliste: List<DsopMeldekort>
)

data class DsopMeldekort(
    val meldekortId: Int,
    val periode: Periode,
    val antallTimerArbeidet: Double,)