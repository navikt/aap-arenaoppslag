package no.nav.aap.arenaoppslag.tilgangsmaskin

import no.nav.aap.arenaoppslag.modeller.PersonId
import no.nav.aap.arenaoppslag.modeller.Saksnummer
import no.nav.aap.arenaoppslag.service.PersonService
import no.nav.aap.arenaoppslag.service.SakService
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken

class TilgangService(
    private val tilgangmaskinGateway: TilgangmaskinGateway,
    private val sakService: SakService,
    private val personService: PersonService,
) {
    /**
     * Access-first: if the token cannot read this identifier, return `AccessDenied` without
     * revealing whether the person exists. If access is granted, resolve the person id and
     * return `NotFound` only for callers who are allowed to know that the object is missing.
     */
    fun verifiserTilgangTilPerson(personIdentifikator: String, token: OidcToken): PersonTilgangResult {
        if (!tilgangmaskinGateway.harTilgangTilPerson(personIdentifikator, token)) {
            return PersonTilgangResult.AccessDenied
        }

        val personId = personService.hentPersonId(personIdentifikator)
            ?: return PersonTilgangResult.NotFound

        return PersonTilgangResult.Granted(AuthorizedPersonId(personId))
    }

    /**
     * Resolves the person behind [saksnummer], then verifies token access to that person.
     * Returns a sealed result so callers can distinguish `NotFound` from `AccessDenied`.
     */
    fun verifiserTilgangTilSak(saksnummer: Saksnummer, token: OidcToken): SakTilgangResult {
        val person = sakService.hentPersonForSak(saksnummer) ?: return SakTilgangResult.NotFound
        return if (tilgangmaskinGateway.harTilgangTilPerson(person.fodselsnummer, token)) {
            SakTilgangResult.Granted(AuthorizedSakId(saksnummer.toString()))
        } else {
            SakTilgangResult.AccessDenied
        }
    }
}

sealed interface PersonTilgangResult {
    data class Granted(val authorizedPersonId: AuthorizedPersonId) : PersonTilgangResult
    data object NotFound : PersonTilgangResult
    data object AccessDenied : PersonTilgangResult
}

sealed interface SakTilgangResult {
    data class Granted(val authorizedSakId: AuthorizedSakId) : SakTilgangResult
    data object NotFound : SakTilgangResult
    data object AccessDenied : SakTilgangResult
}
