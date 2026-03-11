package no.nav.aap.arenaoppslag.modeller

import java.time.LocalDateTime

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

data class ArenaSakMedVedtak (
    val sakId: String,
    val opprettetAar: Int,
    val lopenr: Int,
    val person: ArenaSakPerson,
    val statuskode: String,
    val statusnavn: String,
    val registrertDato: LocalDateTime,
    val avsluttetDato: LocalDateTime?,
    val vedtak: List<ArenaVedtakMedFakta>
)

data class ArenaSakPerson (
    val personId: Int,
    val fodselsnummer: String,
    val fornavn: String,
    val etternavn: String,
)

fun ArenaSak.toArenaSakMedVedtak(vedtak: List<ArenaVedtakMedFakta>) =
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