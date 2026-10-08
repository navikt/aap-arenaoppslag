package no.nav.aap.arenaoppslag.tilgangsmaskin

import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken

interface TilgangmaskinGateway: AutoCloseable {
    suspend fun harTilgangTilPerson(personIdentifikator: String, token: OidcToken): HarTilgangFraTilgangsmaskinen
}

