package no.nav.aap.arenaoppslag.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.arenaoppslag.database.PersonRepository
import no.nav.aap.arenaoppslag.database.SakRepository
import no.nav.aap.arenaoppslag.modeller.ArenaPersonId
import no.nav.aap.arenaoppslag.modeller.PersonIdentifikator
import no.nav.aap.arenaoppslag.modeller.Saksnummer
import no.nav.aap.arenaoppslag.pdl.IPdlGateway
import no.nav.aap.arenaoppslag.pdl.PdlIdent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PersonServiceTest {

    @Test
    fun `hentPersonId med saksnummer slår opp i repository ved cache-miss`() {
        val saksnummer = Saksnummer(lopenummerSak = 1, aar = 2021)
        val sakRepository = mockk<SakRepository>()
        every { sakRepository.hentPersonIdentifikatorHvisEksisterer(saksnummer) } returns PersonIdentifikator("00000000900")
        val service = PersonService(mockk<PersonRepository>(), sakRepository, mockk<IPdlGateway>())

        assertThat(service.hentPersonIdentifikator(saksnummer)).isEqualTo(PersonIdentifikator("00000000900"))

        verify(exactly = 1) { sakRepository.hentPersonIdentifikatorHvisEksisterer(saksnummer) }
    }

    @Test
    fun `hentPersonId med saksnummer bruker cache ved andre kall`() {
        val saksnummer = Saksnummer(lopenummerSak = 1, aar = 2021)
        val sakRepository = mockk<SakRepository>()
        every { sakRepository.hentPersonIdentifikatorHvisEksisterer(saksnummer) } returns PersonIdentifikator("00000000900")
        val service = PersonService(mockk<PersonRepository>(), sakRepository, mockk<IPdlGateway>())

        assertThat(service.hentPersonIdentifikator(saksnummer)).isEqualTo(PersonIdentifikator("00000000900"))
        assertThat(service.hentPersonIdentifikator(saksnummer.copy())).isEqualTo(PersonIdentifikator("00000000900"))

        verify(exactly = 1) { sakRepository.hentPersonIdentifikatorHvisEksisterer(saksnummer) }
    }

    @Test
    fun `hentPersonId med saksnummer cacher ikke null-resultat`() {
        val saksnummer = Saksnummer(lopenummerSak = 1, aar = 2021)
        val sakRepository = mockk<SakRepository>()
        every { sakRepository.hentPersonIdentifikatorHvisEksisterer(saksnummer) } returnsMany listOf(null, PersonIdentifikator("00000000900"))
        val service = PersonService(mockk<PersonRepository>(), sakRepository, mockk<IPdlGateway>())

        assertThat(service.hentPersonIdentifikator(saksnummer)).isNull()
        assertThat(service.hentPersonIdentifikator(saksnummer)).isEqualTo(PersonIdentifikator("00000000900"))
        assertThat(service.hentPersonIdentifikator(saksnummer)).isEqualTo(PersonIdentifikator("00000000900"))

        verify(exactly = 2) { sakRepository.hentPersonIdentifikatorHvisEksisterer(saksnummer) }
    }

    @Test
    fun `hentPersonId cacher uavhengig per år og løpenummer`() {
        val saksnumre = listOf(
            Saksnummer(lopenummerSak = 1, aar = 2021),
            Saksnummer(lopenummerSak = 1, aar = 2022),
            Saksnummer(lopenummerSak = 2, aar = 2021),
        )
        val sakRepository = mockk<SakRepository>()
        saksnumre.forEachIndexed { indeks, saksnummer ->
            every { sakRepository.hentPersonIdentifikatorHvisEksisterer(saksnummer) } returns PersonIdentifikator((indeks + 1).toString().padStart(11, '0'))
        }
        val service = PersonService(mockk<PersonRepository>(), sakRepository, mockk<IPdlGateway>())

        repeat(2) {
            saksnumre.forEachIndexed { indeks, saksnummer ->
                assertThat(service.hentPersonIdentifikator(saksnummer)).isEqualTo(PersonIdentifikator((indeks + 1).toString().padStart(11, '0')))
            }
        }

        saksnumre.forEach { saksnummer ->
            verify(exactly = 1) { sakRepository.hentPersonIdentifikatorHvisEksisterer(saksnummer) }
        }
    }

    @Test
    fun `hentPersonId bruker cache ved andre kall`() {
        val personidentifikator = PersonIdentifikator("12345678901")
        val pdlIdent = "10987654321"
        val pdlGateway = mockk<IPdlGateway>()
        val personRepository = mockk<PersonRepository>()
        val sakRepository = mockk<SakRepository>()

        every { pdlGateway.hentAlleIdenterForPerson(personidentifikator) } returns listOf(
            PdlIdent(ident = personidentifikator.id, historisk = false, gruppe = "FOLKEREGISTERIDENT"),
            PdlIdent(ident = pdlIdent, historisk = true, gruppe = "FOLKEREGISTERIDENT"),
        )
        every { personRepository.hentPersonIdHvisEksisterer(setOf(personidentifikator, PersonIdentifikator(pdlIdent))) } returns ArenaPersonId(1)

        val service = PersonService(personRepository, sakRepository, pdlGateway)

        val firstCall = service.hentPersonIdentifikator(personidentifikator)
        val secondCall = service.hentPersonIdentifikator(personidentifikator)

        assertThat(firstCall).isEqualTo(ArenaPersonId(1))
        assertThat(secondCall).isEqualTo(ArenaPersonId(1))
        verify(exactly = 1) { pdlGateway.hentAlleIdenterForPerson(personidentifikator) }
        verify(exactly = 1) { personRepository.hentPersonIdHvisEksisterer(setOf(personidentifikator, PersonIdentifikator(pdlIdent))) }
    }

    @Test
    fun `hentPersonId cacher ikke null-resultat og gjør nytt oppslag`() {
        val personidentifikator = PersonIdentifikator("12345678901")
        val pdlGateway = mockk<IPdlGateway>()
        val personRepository = mockk<PersonRepository>()
        val sakRepository = mockk<SakRepository>()

        every { pdlGateway.hentAlleIdenterForPerson(personidentifikator) } returns listOf(
            PdlIdent(ident = personidentifikator.id, historisk = false, gruppe = "FOLKEREGISTERIDENT"),
        )
        every { personRepository.hentPersonIdHvisEksisterer(setOf(personidentifikator)) } returnsMany listOf(null, ArenaPersonId(2))

        val service = PersonService(personRepository, sakRepository, pdlGateway)

        val firstCall = service.hentPersonIdentifikator(personidentifikator)
        val secondCall = service.hentPersonIdentifikator(personidentifikator)

        assertThat(firstCall).isNull()
        assertThat(secondCall).isEqualTo(ArenaPersonId(2))
        verify(exactly = 2) { pdlGateway.hentAlleIdenterForPerson(personidentifikator) }
        verify(exactly = 2) { personRepository.hentPersonIdHvisEksisterer(setOf(personidentifikator)) }
    }

    @Test
    fun `hentPersonId cacher uavhengig per personidentifikator`() {
        val personidentifikator1 = PersonIdentifikator("12345678901")
        val personidentifikator2 = PersonIdentifikator("10987654321")
        val pdlGateway = mockk<IPdlGateway>()
        val personRepository = mockk<PersonRepository>()
        val sakRepository = mockk<SakRepository>()

        every { pdlGateway.hentAlleIdenterForPerson(personidentifikator1) } returns listOf(
            PdlIdent(ident = personidentifikator1.id, historisk = false, gruppe = "FOLKEREGISTERIDENT"),
        )
        every { pdlGateway.hentAlleIdenterForPerson(personidentifikator2) } returns listOf(
            PdlIdent(ident = personidentifikator2.id, historisk = false, gruppe = "FOLKEREGISTERIDENT"),
        )
        every { personRepository.hentPersonIdHvisEksisterer(setOf(personidentifikator1)) } returns ArenaPersonId(1)
        every { personRepository.hentPersonIdHvisEksisterer(setOf(personidentifikator2)) } returns ArenaPersonId(2)

        val service = PersonService(personRepository, sakRepository, pdlGateway)

        val firstCallKey1 = service.hentPersonIdentifikator(personidentifikator1)
        val firstCallKey2 = service.hentPersonIdentifikator(personidentifikator2)
        val secondCallKey1 = service.hentPersonIdentifikator(personidentifikator1)
        val secondCallKey2 = service.hentPersonIdentifikator(personidentifikator2)

        assertThat(firstCallKey1).isEqualTo(ArenaPersonId(1))
        assertThat(firstCallKey2).isEqualTo(ArenaPersonId(2))
        assertThat(secondCallKey1).isEqualTo(ArenaPersonId(1))
        assertThat(secondCallKey2).isEqualTo(ArenaPersonId(2))
        verify(exactly = 1) { pdlGateway.hentAlleIdenterForPerson(personidentifikator1) }
        verify(exactly = 1) { pdlGateway.hentAlleIdenterForPerson(personidentifikator2) }
        verify(exactly = 1) { personRepository.hentPersonIdHvisEksisterer(setOf(personidentifikator1)) }
        verify(exactly = 1) { personRepository.hentPersonIdHvisEksisterer(setOf(personidentifikator2)) }
    }
}
