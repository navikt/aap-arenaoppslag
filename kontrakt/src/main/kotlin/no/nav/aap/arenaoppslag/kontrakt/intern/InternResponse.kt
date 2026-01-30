package no.nav.aap.arenaoppslag.kontrakt.intern

import no.nav.aap.arenaoppslag.kontrakt.modeller.Periode
import java.time.LocalDate

public data class PersonEksistererIAAPArena(
    val eksisterer: Boolean
)

public data class SignifikanteSakerResponse(
    val harSignifikantHistorikk: Boolean,
    val signifikanteSaker: List<String> // signifikante Arena-saker, sortert på dato, nyeste først
)

public data class NyereSakerResponse(
    val eksisterer: Boolean,
    val saker: List<String> // moderne Arena-saker, sortert på dato, nyeste først
)

public data class Person(val personIdentifikator: String, val fornavn:String, val etternavn: String)

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
    val statusKode: Status,
    val periode: Periode,
    val kilde: Kilde = Kilde.ARENA
)

public data class ArenaSak(
    val sakId: String,
    val statusKode: String,
    val vedtaktypeKode: String,
    val fraOgMed: LocalDate?,
    val tilDato: LocalDate?,
    val rettighetkode: String
)

public enum class Kilde {
    ARENA,
    KELVIN
}

public enum class Status {
    // Arena:
    AVSLU,
    FORDE,
    GODKJ,
    INNST,
    IVERK,
    KONT,
    MOTAT,
    OPPRE,
    REGIS,

    // Kelvin:
    OPPRETTET,
    UTREDES,
    LØPENDE,
    AVSLUTTET,

    // Begge:
    UKJENT,
}