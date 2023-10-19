package arenaoppslag.fellesordningen

import java.time.LocalDate

data class VedtakRequest(
    val personId: String,
    val datoForOnsketUttakForAFP: LocalDate,
)
