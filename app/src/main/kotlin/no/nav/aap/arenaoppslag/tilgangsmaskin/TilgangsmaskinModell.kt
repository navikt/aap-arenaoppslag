package no.nav.aap.arenaoppslag.tilgangsmaskin

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

data class HarTilgangFraTilgangsmaskinen(
    val harTilgang: Boolean,
    val avvistResponse: TilgangsmaskinAvvistResponse? = null,
)

/**
 * Delmengde av problem+json-svaret Tilgangsmaskinen returnerer ved avslag (403).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class TilgangsmaskinAvvistResponse(
    val type: String,
    val title: String,
    val status: Int,
    val begrunnelse: String,
    val kanOverstyres: Boolean,
)
