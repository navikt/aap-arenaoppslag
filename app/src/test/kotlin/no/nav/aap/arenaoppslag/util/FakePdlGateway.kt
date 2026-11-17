package no.nav.aap.arenaoppslag.util

import no.nav.aap.arenaoppslag.modeller.PersonIdentifikator
import no.nav.aap.arenaoppslag.pdl.IPdlGateway
import no.nav.aap.arenaoppslag.pdl.PdlIdent

class FakePdlGateway : IPdlGateway {
    override fun hentAlleIdenterForPerson(personIdent: PersonIdentifikator): List<PdlIdent> =
        listOf(PdlIdent(ident = personIdent.id, historisk = false, gruppe = "FOLKEREGISTERIDENT"))
}
