package no.nav.aap.arenaoppslag.tilgangsmaskin

import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken

/**
 * Enkel implementasjon som gir tilgang til alle personer.
 */
class FakeTilgangmaskinGateway : TilgangsmaskinGateway {
    override suspend fun harTilgangTilPerson(
        personIdentifikator: String, token: OidcToken
    ): TilgangsmaskinKomplettResponse = TilgangsmaskinKomplettResponse(harTilgang = true)

    override fun close() {
        // no op
    }
}
