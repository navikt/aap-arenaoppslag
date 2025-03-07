package no.nav.aap.arenaoppslag.kontrakt.intern

import no.nav.aap.arenaoppslag.kontrakt.modeller.Periode

public data class personEksistererIAAPArena(
    val eksisterer: Boolean
)

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
    val StatusKode: Status,
    val periode: Periode,
    val kilde:Kilde = Kilde.ARENA
)

public enum class Kilde{
    ARENA,
    KELVIN
}

public enum class Status{
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