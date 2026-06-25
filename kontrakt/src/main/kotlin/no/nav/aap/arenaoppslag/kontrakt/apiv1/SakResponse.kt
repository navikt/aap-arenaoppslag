package no.nav.aap.arenaoppslag.kontrakt.apiv1

import java.time.LocalDateTime

public data class ArenaSakPersonKontrakt(
    val personId: Int,
    val fodselsnummer: String,
    val fornavn: String,
    val etternavn: String,
)

public data class ArenaSakMedVedtakResponse(
    val sakId: String,
    val opprettetAar: Int,
    val lopenr: Int,
    val person: ArenaSakPersonKontrakt,
    val statuskode: String,
    val statusnavn: String,
    val registrertDato: LocalDateTime,
    val avsluttetDato: LocalDateTime?,
    val vedtak: List<ArenaVedtakMedDetaljer>,
)
