package no.nav.aap.arenaoppslag.kontrakt.intern

import java.time.LocalDate

public data class InternVedtakRequest(
    val personidentifikator: String,
    val fraOgMedDato: LocalDate,
    val tilOgMedDato: LocalDate
)

public data class SakerRequest (
    val personidentifikator: List<String>
)