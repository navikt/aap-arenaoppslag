package no.nav.aap.arenaoppslag.kontrakt.intern

import java.time.LocalDate

public data class InternVedtakRequest(
    val personidentifikator: String,
    val fraOgMedDato: LocalDate = LocalDate.MIN,
    val tilOgMedDato: LocalDate = LocalDate.MAX
)

public data class SakerRequest (
    val personidentifikatorer: List<String>
)