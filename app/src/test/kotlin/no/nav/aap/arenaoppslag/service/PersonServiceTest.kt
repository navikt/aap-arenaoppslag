package no.nav.aap.arenaoppslag.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.arenaoppslag.database.PersonRepository
import no.nav.aap.arenaoppslag.modeller.PersonId
import no.nav.aap.arenaoppslag.pdl.IPdlGateway
import no.nav.aap.arenaoppslag.pdl.PdlIdent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PersonServiceTest {

    @Test
    fun `hentPersonId bruker cache ved andre kall`() {
        val personidentifikator = "12345678901"
        val pdlIdent = "10987654321"
        val pdlGateway = mockk<IPdlGateway>()
        val personRepository = mockk<PersonRepository>()

        every { pdlGateway.hentAlleIdenterForPerson(personidentifikator) } returns listOf(
            PdlIdent(ident = personidentifikator, historisk = false, gruppe = "FOLKEREGISTERIDENT"),
            PdlIdent(ident = pdlIdent, historisk = true, gruppe = "FOLKEREGISTERIDENT"),
        )
        every { personRepository.hentPersonIdHvisEksisterer(setOf(personidentifikator, pdlIdent)) } returns PersonId(1)

        val service = PersonService(personRepository, pdlGateway)

        val firstCall = service.hentPersonId(personidentifikator)
        val secondCall = service.hentPersonId(personidentifikator)

        assertThat(firstCall).isEqualTo(PersonId(1))
        assertThat(secondCall).isEqualTo(PersonId(1))
        verify(exactly = 1) { pdlGateway.hentAlleIdenterForPerson(personidentifikator) }
        verify(exactly = 1) { personRepository.hentPersonIdHvisEksisterer(setOf(personidentifikator, pdlIdent)) }
    }

    @Test
    fun `hentPersonId cacher ikke null-resultat og gjør nytt oppslag`() {
        val personidentifikator = "12345678901"
        val pdlGateway = mockk<IPdlGateway>()
        val personRepository = mockk<PersonRepository>()

        every { pdlGateway.hentAlleIdenterForPerson(personidentifikator) } returns listOf(
            PdlIdent(ident = personidentifikator, historisk = false, gruppe = "FOLKEREGISTERIDENT"),
        )
        every { personRepository.hentPersonIdHvisEksisterer(setOf(personidentifikator)) } returnsMany listOf(null, PersonId(2))

        val service = PersonService(personRepository, pdlGateway)

        val firstCall = service.hentPersonId(personidentifikator)
        val secondCall = service.hentPersonId(personidentifikator)

        assertThat(firstCall).isNull()
        assertThat(secondCall).isEqualTo(PersonId(2))
        verify(exactly = 2) { pdlGateway.hentAlleIdenterForPerson(personidentifikator) }
        verify(exactly = 2) { personRepository.hentPersonIdHvisEksisterer(setOf(personidentifikator)) }
    }

    @Test
    fun `hentPersonId cacher uavhengig per personidentifikator`() {
        val personidentifikator1 = "12345678901"
        val personidentifikator2 = "10987654321"
        val pdlGateway = mockk<IPdlGateway>()
        val personRepository = mockk<PersonRepository>()

        every { pdlGateway.hentAlleIdenterForPerson(personidentifikator1) } returns listOf(
            PdlIdent(ident = personidentifikator1, historisk = false, gruppe = "FOLKEREGISTERIDENT"),
        )
        every { pdlGateway.hentAlleIdenterForPerson(personidentifikator2) } returns listOf(
            PdlIdent(ident = personidentifikator2, historisk = false, gruppe = "FOLKEREGISTERIDENT"),
        )
        every { personRepository.hentPersonIdHvisEksisterer(setOf(personidentifikator1)) } returns PersonId(1)
        every { personRepository.hentPersonIdHvisEksisterer(setOf(personidentifikator2)) } returns PersonId(2)

        val service = PersonService(personRepository, pdlGateway)

        val firstCallKey1 = service.hentPersonId(personidentifikator1)
        val firstCallKey2 = service.hentPersonId(personidentifikator2)
        val secondCallKey1 = service.hentPersonId(personidentifikator1)
        val secondCallKey2 = service.hentPersonId(personidentifikator2)

        assertThat(firstCallKey1).isEqualTo(PersonId(1))
        assertThat(firstCallKey2).isEqualTo(PersonId(2))
        assertThat(secondCallKey1).isEqualTo(PersonId(1))
        assertThat(secondCallKey2).isEqualTo(PersonId(2))
        verify(exactly = 1) { pdlGateway.hentAlleIdenterForPerson(personidentifikator1) }
        verify(exactly = 1) { pdlGateway.hentAlleIdenterForPerson(personidentifikator2) }
        verify(exactly = 1) { personRepository.hentPersonIdHvisEksisterer(setOf(personidentifikator1)) }
        verify(exactly = 1) { personRepository.hentPersonIdHvisEksisterer(setOf(personidentifikator2)) }
    }
}