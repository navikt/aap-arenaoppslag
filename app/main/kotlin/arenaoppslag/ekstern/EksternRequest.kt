package arenaoppslag.ekstern

import java.time.LocalDate

data class VedtakRequest(
    val personidentifikator: String,
    val fraOgMedDato: LocalDate,
    val tilOgMedDato: LocalDate
)
