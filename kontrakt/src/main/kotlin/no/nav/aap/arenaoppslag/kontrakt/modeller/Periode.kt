package no.nav.aap.arenaoppslag.kontrakt.modeller

import java.time.LocalDate

/**
 * At [tilOgMedDato] < [fraOgMedDato] betyr at vedtaket er ugyldiggjort.
 *
 * @param fraOgMedDato Kan være null på avslåtte førstegangsbehandlinger og på nylig mottatte søknader (de med status MOTAT og OPPRE).
 */
public data class Periode(
    val fraOgMedDato: LocalDate?,
    val tilOgMedDato: LocalDate?
)
