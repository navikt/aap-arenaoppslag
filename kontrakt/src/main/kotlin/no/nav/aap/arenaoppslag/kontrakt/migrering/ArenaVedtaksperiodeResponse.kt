package no.nav.aap.arenaoppslag.kontrakt.migrering

import java.time.LocalDate

public data class ArenaVedtaksperiodeResponse(
    val vedtakId: Int,
    val fraDato: LocalDate?,
    val tilDato: LocalDate?,
)
