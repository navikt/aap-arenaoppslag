package no.nav.aap.arenaoppslag.kontrakt.migrering

import java.time.LocalDate

public data class ArenaRefusjonskravResponse(
    val refusjonskrav: ArenaRefusjonskrav?,
)

public data class ArenaRefusjonskrav(
    val aarsak: String,
    val fraDato: LocalDate?,
    val tilDato: LocalDate?,
)
