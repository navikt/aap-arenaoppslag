package no.nav.aap.arenaoppslag.tilgang

import no.nav.aap.arenaoppslag.modeller.PersonIdentifikator
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.tilgang.TilgangResponse

/**
 * Enkel implementasjon som gir tilgang til alle personer.
 */
class FakeTilgangGateway : TilgangGateway {
    override suspend fun harTilgangTilPerson(
        personIdentifikator: PersonIdentifikator, token: OidcToken
    ): TilgangResponse = TilgangResponse(tilgang = true)

    override fun close() {
        // no op
    }
}
