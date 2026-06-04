package no.nav.aap.arenaoppslag.tilgangsmaskin

import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken

interface TilgangmaskinGateway {
    fun harTilgangTilPerson(personIdentifikator: String, token: OidcToken): Boolean
}