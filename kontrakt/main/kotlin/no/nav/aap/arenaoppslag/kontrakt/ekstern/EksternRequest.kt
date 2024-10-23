package no.nav.aap.arenaoppslag.kontrakt.ekstern

import java.time.LocalDate

public data class EksternVedtakRequest(
    val personidentifikator: String,
    val fraOgMedDato: LocalDate,
    val tilOgMedDato: LocalDate
)
