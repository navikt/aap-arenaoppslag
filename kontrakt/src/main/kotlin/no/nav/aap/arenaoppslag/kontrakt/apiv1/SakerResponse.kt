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
    val statuskode: String,
    val statusnavn: String,
    val sakstype: String?,
    val regDato: LocalDate,
    val avsluttetDato: LocalDate?,
)

public data class SakMedSisteVedtakOgMaksdato(
    val sakId: Int,
    val sakStatus: String,
    val sakRegistrert: LocalDate,
    val sakAvsluttet: LocalDate?,
    val har_11_12_forlengelse: Boolean,
    val utredesForUfor: Boolean,
    val lopende: Boolean,
    val sisteVedtak: VedtakMedMaksdato
)

public data class VedtakMedMaksdato(
    val vedtakId: Int,
    val aktfaseKode: String,
    val vedtaktypeKode: String,
    val fra: LocalDate?,
    val maxUnntakTil: LocalDate?
)

public data class MaksdatoResponse(val sakliste: List<SakMedSisteVedtakOgMaksdato>)

public data class SisteUtbetalingerRequest(
    val fodselsnummer: String
)

public data class SisteUtbetalingerResponse(val sakliste: List<SakMedSisteUtbetaling>)

public data class SakMedSisteUtbetaling(val sakId: Int, val sisteAAPUtbetalingsdato: LocalDate?)

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
