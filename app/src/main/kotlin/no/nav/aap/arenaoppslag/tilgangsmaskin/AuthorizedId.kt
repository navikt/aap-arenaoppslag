package no.nav.aap.arenaoppslag.tilgangsmaskin

import no.nav.aap.arenaoppslag.modeller.PersonId

/**
 * Proof that the caller has been authorized for a specific person, and carries the resolved
 * internal [PersonId] for downstream service/repository calls.
 *
 */
data class AuthorizedPersonId(val personId: PersonId)

/**
 * Proof that the caller has been authorized for a specific sak.
 */
data class AuthorizedSakId(val saksnummer: String)
