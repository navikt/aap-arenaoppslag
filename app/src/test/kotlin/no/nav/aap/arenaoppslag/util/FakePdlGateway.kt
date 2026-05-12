package no.nav.aap.arenaoppslag.util

import no.nav.aap.arenaoppslag.pdl.IPdlGateway
import no.nav.aap.arenaoppslag.pdl.PdlIdent

class FakePdlGateway : IPdlGateway {
    override fun hentAlleIdenterForPerson(personIdent: String): List<PdlIdent> =
        listOf(PdlIdent(ident = personIdent, historisk = false, gruppe = "FOLKEREGISTERIDENT"))
}

