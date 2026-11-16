package no.nav.aap.arenaoppslag.tilgangsmaskin

import no.nav.aap.arenaoppslag.modeller.PersonId
import no.nav.aap.arenaoppslag.modeller.Saksnummer


sealed interface AuthorisertPersonId {
    val personId: PersonId
}

sealed interface AutorisertSaksnummer {
    val saksnummer: Saksnummer
}
