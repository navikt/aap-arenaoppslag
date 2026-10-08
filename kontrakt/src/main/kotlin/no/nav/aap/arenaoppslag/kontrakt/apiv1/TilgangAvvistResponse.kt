package no.nav.aap.arenaoppslag.kontrakt.apiv1

public data class TilgangAvvistResponse(
    val title: String,
    val begrunnelse: String,
    val kanOverstyres: Boolean,
)
