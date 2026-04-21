package no.nav.aap.arenaoppslag.kontrakt.apiv1

import java.time.LocalDate

public data class SakerRequest(
    val personidentifikator: String
)

public data class MaksdatoRequest(
    val saker: List<Int>
)

public data class SakerResponse(
    val saker: List<ArenaSakOppsummeringKontrakt>
)

public data class ArenaSakOppsummeringKontrakt(
    val sakId: String,
    val lopenummer: Int,
    val aar: Int,
    val antallVedtak: Int,
    val sakstype: String?,
    val regDato: LocalDate,
    val avsluttetDato: LocalDate?,
)

public data class Maksdatolinje(
    val sakId: Int,
    val vedtakId: Int,
    val aktfaseKode: String,
    val maxUnntakDato: LocalDate?
)

public data class MaksdatoResponse(val sakliste: List<Maksdatolinje>)

// SakstypeKontrakt er foreløpig ikke i bruk, men beholdes for fremtidig bruk med enum-basert sakstype
public enum class SakstypeKontrakt {
    AA,
    ARBEID,
    ATTF,
    DAGP,
    ENSLIG,
    FEILUTBE,
    INDIV,
    KLAN,
    MOBIL,
    REHAB,
    SANKSJON,
    SANKSJON_A,
    SANKSJON_B,
    SYKEP,
    TILSTOVER,
    TILSTRAMME,
    TILT,
    UFOREYT,
    UTRSYA,
    VLONN,
}
