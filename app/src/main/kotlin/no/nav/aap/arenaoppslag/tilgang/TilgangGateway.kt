package no.nav.aap.arenaoppslag.tilgang

import no.nav.aap.arenaoppslag.modeller.PersonIdentifikator
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.tilgang.TilgangResponse

interface TilgangGateway: AutoCloseable {
    suspend fun harTilgangTilPerson(personIdentifikator: PersonIdentifikator, token: OidcToken): TilgangResponse
}

