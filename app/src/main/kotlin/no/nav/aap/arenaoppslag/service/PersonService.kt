package no.nav.aap.arenaoppslag.service

import no.nav.aap.arenaoppslag.IPdlGateway
import no.nav.aap.arenaoppslag.database.PersonRepository
import no.nav.aap.arenaoppslag.modeller.PersonId

class PersonService(
    private val personRepository: PersonRepository,
    private val pdlGateway: IPdlGateway,
) {
    fun hentPersonId(personidentifikator: String): PersonId? {
        val alleIdenter = pdlGateway.hentAlleIdenterForPerson(personidentifikator)
            .map { it.ident }
            .toSet()
        return personRepository.hentPersonIdHvisEksisterer(alleIdenter)
    }
}
