package no.nav.aap.arenaoppslag.tilgangsmaskin

import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken

/**
 * Midlertidig implementasjon som gir tilgang til alle personer.
 * Må erstattes med et ekte HTTP-kall til Tilgangsmaskin før bruk i produksjon.
 */
class TilgangmaskinGatewayMockImpl : TilgangmaskinGateway {
    override fun harTilgangTilPerson(personIdentifikator: String, token: OidcToken): Boolean = true
}
