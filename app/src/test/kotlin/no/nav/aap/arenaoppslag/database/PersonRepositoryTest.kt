package no.nav.aap.arenaoppslag.database

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import kotlin.test.assertNull

class PersonRepositoryTest : H2TestBase("flyway/minimumtest", "flyway/eksisterer") {

    private lateinit var personRepository: PersonRepository

    @BeforeEach
    fun setUp() {
        personRepository = PersonRepository(h2)
    }

    @Test
    fun `person eksisterer`() {
        val personEksisterer = personRepository.hentPersonIdHvisEksisterer(setOf("1"))
        assertNotNull(personEksisterer)
        val personEksistererIkke = personRepository.hentPersonIdHvisEksisterer(setOf("2012012031"))
        assertNull(personEksistererIkke)
    }

}
