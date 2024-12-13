package no.nav.aap.arenaoppslag.kontrakt.modeller

import java.time.LocalDate

public data class Periode(
    val fraOgMedDato: LocalDate?,
    val tilOgMedDato: LocalDate?
)