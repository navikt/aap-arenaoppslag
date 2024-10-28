package no.nav.aap.arenaoppslag.kontrakt.dsop

public data class VedtakResponse(
    val uttrekksperiode: Periode,
    val vedtaksliste: List<DsopVedtak>
)

public data class MeldekortResponse(
    val uttrekksperiode: Periode,
    val meldekortliste: List<DsopMeldekort>
)

public data class DsopVedtak(
    val vedtakId: Int,
    val virkningsperiode: Periode,
    val vedtakstype: Kodeverdi,
    val vedtaksvariant: Kodeverdi,
    val vedtakstatus: Kodeverdi,
    val rettighetstype: Kodeverdi,
    val utfall: Kodeverdi,
    val aktivitetsfase: Kodeverdi,
)

public data class DsopMeldekort(
    val meldekortId: Int,
    val periode: Periode,
    val antallTimerArbeidet: Double
)

public data class Kodeverdi(
    val kode: String,
    val termnavn: String
)
