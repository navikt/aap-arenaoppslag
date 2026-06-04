package no.nav.aap.arenaoppslag.tilgangsmaskin

import no.nav.aap.arenaoppslag.modeller.PersonId
import no.nav.aap.arenaoppslag.modeller.Saksnummer
import no.nav.aap.arenaoppslag.service.SakService
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken

class TilgangService(
    private val tilgangmaskinGateway: TilgangmaskinGateway,
    private val sakService: SakService,
) {
    /**
     * Returns an [AuthorizedPersonId] if the token is allowed to read data for [personIdentifikator].
     * The returned object carries the resolved internal [PersonId] so downstream code can use it directly.
     */
    fun verifiserTilgangTilPerson(personIdentifikator: String, personId: PersonId, token: OidcToken): AuthorizedPersonId? {
        if (!tilgangmaskinGateway.harTilgangTilPerson(personIdentifikator, token)) return null
        return AuthorizedPersonId(personId)
    }

    /**
     * Resolves the person behind [saksnummer], then verifies token access to that person.
     * Returns null if the sak is not found or access is denied.
     */
    fun verifiserTilgangTilSak(saksnummer: Saksnummer, token: OidcToken): AuthorizedSakId? {
        val person = sakService.hentPersonForSak(saksnummer) ?: return null
        return if (tilgangmaskinGateway.harTilgangTilPerson(person.fodselsnummer, token))
            AuthorizedSakId(saksnummer.toString())
        else
            null
    }
}
