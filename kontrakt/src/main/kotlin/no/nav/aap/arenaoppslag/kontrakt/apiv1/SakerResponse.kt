package no.nav.aap.arenaoppslag.kontrakt.apiv1

import java.time.LocalDate

public data class SakerRequest(
    val personidentifikator: String
)

public data class MaksdatoRequest(
    val personidentifikator: String
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
    val saknummer: String,
    val sakStatus: String,
    val sakRegistrert: LocalDate,
    val sakAvsluttet: LocalDate?,
    val har_11_12_forlengelse: Boolean,
    val utredesForUfor: Boolean,
    val ferdigAvklart: Boolean,
    val lopendeVedtak: Boolean,
    val sisteVedtak: VedtakMedMaksdato,
) {
    public fun medUdefinertMaxsdato(): SakMedSisteVedtakOgMaksdato {
        val sisteVedtakKopi = this.sisteVedtak.copy(maxdatoAap = null, maxdatoUnntak = null, maxdatoOrdinaer = null)

        return this.copy(sisteVedtak = sisteVedtakKopi)
    }
}

public data class VedtakMedMaksdato(
    val vedtakId: Int,
    val aktfaseKode: String,
    val vedtaktypeKode: String,
    val fra: LocalDate?,
    val til: LocalDate?,
    val maxdatoOrdinaer: LocalDate?,
    val maxdatoUnntak: LocalDate?,
    val maxdatoAap: LocalDate?,
)

public data class MaksdatoResponse(val sakliste: List<SakMedSisteVedtakOgMaksdato>)

public data class SisteUtbetalingerRequest(
    val personidentifikator: String
)

public data class SisteUtbetalingerResponse(val utbetalingsdato: LocalDate?)

public data class SakMedSisteUtbetaling(val sakId: Int, val sisteAAPUtbetalingsdato: LocalDate?)

// SakstypeKontrakt er foreløpig ikke i bruk, men beholdes for fremtidig bruk med enum-basert sakstype
public enum class Sakstype {
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
