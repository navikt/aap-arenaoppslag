package no.nav.aap.arenaoppslag.tilgangsmaskin

import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken

interface TilgangsmaskinGateway: AutoCloseable {
    suspend fun harTilgangTilPerson(personIdentifikator: String, token: OidcToken): TilgangsmaskinKomplettResponse
}

