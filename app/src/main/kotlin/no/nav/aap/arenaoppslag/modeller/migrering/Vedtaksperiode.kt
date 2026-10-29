package no.nav.aap.arenaoppslag.modeller.migrering

import no.nav.aap.arenaoppslag.kontrakt.migrering.ArenaVedtaksperiodeResponse
import java.time.LocalDate

data class Vedtaksperiode(
    val vedtakId: Int,
    val fraDato: LocalDate?,
    val tilDato: LocalDate?,
) {
    fun tilKontrakt() = ArenaVedtaksperiodeResponse(
        vedtakId = vedtakId,
        fraDato = fraDato,
        tilDato = tilDato,
    )
}
