package no.nav.aap.arenaoppslag.database

import no.nav.aap.arenaoppslag.modeller.PersonIdentifikator
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import kotlin.test.assertNull

class PersonRepositoryTest : H2TestBase("flyway/eksisterer") {

    private lateinit var personRepository: PersonRepository

    @BeforeEach
    fun setUp() {
        personRepository = PersonRepository(h2)
    }

    @Test
    fun `person eksisterer`() {
        val personEksisterer = personRepository.hentPersonIdHvisEksisterer(setOf(PersonIdentifikator("12312312312")))
        assertNotNull(personEksisterer)
        val personEksistererIkke = personRepository.hentPersonIdHvisEksisterer(setOf(PersonIdentifikator("2012012031")))
        assertNull(personEksistererIkke)
    }

    @Test
    fun `person eksisterer men er merket som duplikat`() {
        val personEksisterer = personRepository.hentPersonIdHvisEksisterer(setOf("korrekt", "duplikat", "en ekstra").map { PersonIdentifikator(it) }.toSet())
        assertNotNull(personEksisterer) // skal returneres uten feil
    }

}
