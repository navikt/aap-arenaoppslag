package no.nav.aap.arenaoppslag.tilgangsmaskin

import no.nav.aap.arenaoppslag.modeller.PersonId
import no.nav.aap.arenaoppslag.modeller.Saksnummer


/**
 * Bevis på at kalleren er autorisert for tilgang til data om en bestemt person, og inneholder den oppslåtte
 * interne [PersonId]-en som brukes ved videre kall til tjenester og repositorier.
 * Private constructor slik at det ikke opprettes instanser uten at det er gjennomtenkt av utvikleren.
 */
class AuthorisertPersonId private constructor(val personId: PersonId) {
    companion object {
        fun createInstance(personId: PersonId): AuthorisertPersonId {
            return AuthorisertPersonId(personId)
        }
    }
}

/**
 * Bevis på at kalleren er autorisert for tilgang til en bestemt sak.
 * Private constructor slik at det ikke opprettes instanser uten at det er gjennomtenkt av utvikleren.
 */
data class AutorisertSaksnummer private constructor(val saksnummer: String) {
    companion object {
        fun createInstance(saksnummer: String): AutorisertSaksnummer {
            return AutorisertSaksnummer(saksnummer)
        }
    }

    fun toSaksnummer() = Saksnummer.fromString(saksnummer)!!
}
