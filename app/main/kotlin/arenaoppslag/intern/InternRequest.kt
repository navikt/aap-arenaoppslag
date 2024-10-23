package arenaoppslag.intern

import java.time.LocalDate

data class VedtakRequest(
    val personidentifikator: String,
    val fraOgMedDato: LocalDate,
    val tilOgMedDato: LocalDate
)

class SakerRequest (
    val personidentifikator: String
)