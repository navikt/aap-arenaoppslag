package no.nav.aap.arenaoppslag.tilgang

import no.nav.aap.arenaoppslag.modeller.ArenaPersonId
import no.nav.aap.arenaoppslag.modeller.Saksnummer


sealed interface AuthorisertPersonId {
    val personId: ArenaPersonId
}

sealed interface AutorisertSaksnummer {
    val saksnummer: Saksnummer
}
