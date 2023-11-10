package arenaoppslag.dsop

data class VedtakResponse(
    val uttrekksperiode: Periode,
    val vedtaksliste: List<DsopVedtak>
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
