package no.nav.aap.arenaoppslag.tilgangsmaskin

import no.nav.aap.arenaoppslag.modeller.PersonId

/**
 * Proof that the caller has been authorized for a specific person, and carries the resolved
 * internal [PersonId] for downstream service/repository calls.
 *
 * Use with Kotlin 2.3 context parameters:
 * ```
 * val authorized = tilgangService.verifiserTilgangTilPerson(fnr, token) ?: return Forbidden
 * context(authorized) {
 *     vedtakRepository.hentVedtak(authorized.personId)
 * }
 * ```
 */
data class AuthorizedPersonId(val personId: PersonId)

/**
 * Proof that the caller has been authorized for a specific sak.
 */
data class AuthorizedSakId(val saksnummer: String)
