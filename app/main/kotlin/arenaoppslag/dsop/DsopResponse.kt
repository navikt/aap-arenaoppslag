package arenaoppslag.dsop

import java.time.LocalDate

data class VedtakResponse(
    val uttrekksperiode: Periode,
    val vedtaksliste: List<DsopVedtak>
)

data class MeldekortResponse(
    val uttrekksperiode: Periode,
    val meldekortliste: List<DsopMeldekort>
)

data class DsopVedtak(
    val vedtakId: Int,
    val virkningsperiode: Periode,
    val vedtakstype: Kodeverdi,
    val vedtaksvariant: Kodeverdi,
    val vedtakstatus: Kodeverdi,
    val rettighetstype: Kodeverdi,
    val utfall: Kodeverdi,
    val aktivitetsfase: Kodeverdi,
)

data class DsopMeldekort(
    val meldekortId: Int,
    val periode: Periode,
    val antallTimerArbeidet: Double
)

data class Kodeverdi(
    val kode: String,
    val termnavn: String
)

data class Periode(
    val fraDato: LocalDate,
    val tilDato: LocalDate
)
