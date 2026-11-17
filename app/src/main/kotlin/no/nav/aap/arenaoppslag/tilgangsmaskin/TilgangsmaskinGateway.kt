package no.nav.aap.arenaoppslag.tilgangsmaskin

import no.nav.aap.arenaoppslag.modeller.PersonIdentifikator
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken

interface TilgangsmaskinGateway: AutoCloseable {
    suspend fun harTilgangTilPerson(personIdentifikator: PersonIdentifikator, token: OidcToken): TilgangsmaskinKomplettResponse
}

