package no.nav.aap.arenaoppslag.kontrakt.intern

import java.time.LocalDate

public data class InternVedtakRequest(
    val personidentifikator: String,
    val fraOgMedDato: LocalDate = LocalDate.MIN,
    val tilOgMedDato: LocalDate = LocalDate.MAX
)

public data class SakerRequest(
    val personidentifikatorer: List<String>
)

public data class TellerRequest(
    val personidentifikator: String
)

public data class SignifikanteSakerRequest(
    val personidentifikatorer: List<String>,
    val virkningstidspunkt: LocalDate, // datoen søknaden ble mottatt, feks. per post
)
