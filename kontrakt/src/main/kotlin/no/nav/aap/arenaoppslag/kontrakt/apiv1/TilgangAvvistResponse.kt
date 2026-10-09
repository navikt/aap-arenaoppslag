package no.nav.aap.arenaoppslag.kontrakt.apiv1

/**
 * Se Confluence for dokumentasjon.
 * https://confluence.adeo.no/spaces/TM/pages/628888614/Intro+til+Tilgangsmaskinen
 * og schema i Swagger for dokumentasjon av felt.
 */
public data class TilgangAvvistResponse(
    val title: String,
    val begrunnelse: String,
    val kanOverstyres: Boolean,
)
