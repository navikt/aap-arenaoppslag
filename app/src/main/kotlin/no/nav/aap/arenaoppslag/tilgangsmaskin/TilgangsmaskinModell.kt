package no.nav.aap.arenaoppslag.tilgangsmaskin

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

data class HarTilgangFraTilgangsmaskinen(
    val harTilgang: Boolean,
    val avvistResponse: TilgangsmaskinAvvistResponse? = null,
)

/**
 * Delmengde av problem+json-svaret Tilgangsmaskinen returnerer ved avslag (403).
 * Feltene er nullbare fordi vi tolker svaret defensivt og ikke vil feile om tjenesten
 * utelater felt eller endrer formatet.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class TilgangsmaskinAvvistResponse(
    val type: String? = null,
    val title: String? = null,
    val status: Int? = null,
    val begrunnelse: String? = null,
    val kanOverstyres: Boolean? = null,
)
