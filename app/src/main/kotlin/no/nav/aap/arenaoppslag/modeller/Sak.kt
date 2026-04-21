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

data class Maksdatolinje(val sakId: Int, val vedtakId: Int, val maxUnntakDato: LocalDate?, val aktfaseKode: String) {
    fun tilKontrakt() =
        no.nav.aap.arenaoppslag.kontrakt.apiv1.Maksdatolinje(this.sakId, this.vedtakId, this.aktfaseKode, this.maxUnntakDato)
}

data class ArenaSakMedVedtak (
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

data class ArenaSakPerson (
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