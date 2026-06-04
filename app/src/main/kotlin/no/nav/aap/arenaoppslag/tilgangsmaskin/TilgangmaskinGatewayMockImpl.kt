package no.nav.aap.arenaoppslag.tilgangsmaskin

import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken

class TilgangmaskinGatewayMockImpl: TilgangmaskinGateway {
    override fun harTilgangTilPerson(
        personIdentifikator: String,
        token: OidcToken
    ): Boolean {
        return true
    }
}