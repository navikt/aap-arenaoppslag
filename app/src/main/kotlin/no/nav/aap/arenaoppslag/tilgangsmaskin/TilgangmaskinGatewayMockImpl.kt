package no.nav.aap.arenaoppslag.tilgangsmaskin

import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken

/**
 * Placeholder implementation that grants access to all persons.
 * Replace with a real HTTP call to Tilgangsmaskin before production use.
 */
class TilgangmaskinGatewayMockImpl : TilgangmaskinGateway {
    override fun harTilgangTilPerson(personIdentifikator: String, token: OidcToken): Boolean = true
}

