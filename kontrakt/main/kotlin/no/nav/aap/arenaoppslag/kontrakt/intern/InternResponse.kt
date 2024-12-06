package no.nav.aap.arenaoppslag.kontrakt.intern

import no.nav.aap.arenaoppslag.kontrakt.modeller.Periode


public data class VedtakResponse(
    val perioder: List<Periode>
)

public data class PerioderMed11_17Response(
    val perioder: List<PeriodeMed11_17>
)

public data class PeriodeMed11_17(
    val periode: Periode,
    val aktivitetsfaseKode: String,
    val aktivitetsfaseNavn: String
)

public data class SakStatus(
    val sakId: String,
    val vedtakStatusKode: VedtakStatus,
    val periode: Periode
)

public enum class VedtakStatus{
    AVSLU,
    FORDE,
    GODKJ,
    INNST,
    IVERK,
    KONT,
    MOTAT,
    OPPRE,
    REGIS,
    UKJENT
}