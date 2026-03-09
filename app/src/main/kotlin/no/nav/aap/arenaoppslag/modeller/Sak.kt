package no.nav.aap.arenaoppslag.modeller

import java.time.LocalDateTime

data class ArenaSak(
    val sakId: String,
    val opprettetAar: Int,
    val lopenr: Int,
    val fodselsnummer: String,
    val statuskode: String,
    val registrertDato: LocalDateTime,
    val avsluttetDato: LocalDateTime?,
)

data class ArenaSakMedVedtak (
    val sakId: String,
    val opprettetAar: Int,
    val lopenr: Int,
    val fodselsnummer: String,
    val statuskode: String,
    val registrertDato: LocalDateTime,
    val avsluttetDato: LocalDateTime?,
    val vedtak: List<ArenaVedtakMedFakta>
)

fun ArenaSak.toArenaSakMedVedtak(vedtak: List<ArenaVedtakMedFakta>) =
    ArenaSakMedVedtak(
        sakId = sakId,
        opprettetAar = opprettetAar,
        lopenr = lopenr,
        fodselsnummer = fodselsnummer,
        registrertDato = registrertDato,
        avsluttetDato = avsluttetDato,
        statuskode = statuskode,
        vedtak = vedtak
    )