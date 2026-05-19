package no.nav.aap.arenaoppslag.modeller

import no.nav.aap.arenaoppslag.kontrakt.apiv1.ArenaSakOppsummeringKontrakt
import java.time.LocalDate
import java.time.LocalDateTime


data class ArenaSakOppsummering(
    val sakId: String,
    val lopenummer: Int,
    val aar: Int,
    val antallVedtak: Int,
    val sakstype: String?,
    val statuskode: String,
    val statusnavn: String,
    val regDato: LocalDate,
    val avsluttetDato: LocalDate?,
) {
    fun tilKontrakt() = ArenaSakOppsummeringKontrakt(
        sakId = sakId,
        lopenummer = lopenummer,
        aar = aar,
        antallVedtak = antallVedtak,
        sakstype = sakstype,
        regDato = regDato,
        avsluttetDato = avsluttetDato,
        statuskode = statuskode,
        statusnavn = statusnavn,
    )
}

data class ArenaSak(
    val sakId: String,
    val opprettetAar: Int,
    val lopenr: Int,
    val person: ArenaSakPerson,
    val statuskode: String,
    val statusnavn: String,
    val registrertDato: LocalDateTime,
    val avsluttetDato: LocalDateTime?,
)

data class Maksdatolinje(
    val sakId: Int,
    val vedtakId: Int,
    val aktfaseKode: String,
    val vedtaktypeKode: String,
    val fra: LocalDate?,
    val maxdatoUnntak: LocalDate?,
    val maxdato: LocalDate?,
    val utvidetKvoteInnvilgetFra: LocalDate?,
    val sakRegistrert: LocalDate,
    val sakAvsluttet: LocalDate?,
    val sakStatus: String,
) {
    fun tilKontrakt() =
        no.nav.aap.arenaoppslag.kontrakt.apiv1.SakMedSisteVedtakOgMaksdato(
            sakId, sakStatus, sakRegistrert, sakAvsluttet,
            harInnvilget11_12(),
            utredesForUfor(),
            erFerdigAvklart(),
            erLopende(),
            no.nav.aap.arenaoppslag.kontrakt.apiv1.VedtakMedMaksdato(
                vedtakId,
                aktfaseKode,
                vedtaktypeKode,
                fra,
                maxdatoUnntak ?: maxdato
            )
        )

    fun erLopende() = sakStatus == "AKTIV" && vedtaktypeKode in listOf("O", "E", "G")
    fun utredesForUfor() = aktfaseKode == "UVUP"
    fun erFerdigAvklart() = aktfaseKode == "FA"
    fun harInnvilget11_12() = utvidetKvoteInnvilgetFra != null
}

data class ArenaSakMedVedtak(
    val sakId: String,
    val opprettetAar: Int,
    val lopenr: Int,
    val person: ArenaSakPerson,
    val statuskode: String,
    val statusnavn: String,
    val registrertDato: LocalDateTime,
    val avsluttetDato: LocalDateTime?,
    val vedtak: List<ArenaVedtakMedDetaljer>
)

data class ArenaSakPerson(
    val personId: Int,
    val fodselsnummer: String,
    val fornavn: String,
    val etternavn: String,
)

fun ArenaSak.toArenaSakMedVedtak(vedtak: List<ArenaVedtakMedDetaljer>) =
    ArenaSakMedVedtak(
        sakId = sakId,
        opprettetAar = opprettetAar,
        lopenr = lopenr,
        person = person,
        registrertDato = registrertDato,
        avsluttetDato = avsluttetDato,
        statuskode = statuskode,
        statusnavn = statusnavn,
        vedtak = vedtak
    )
