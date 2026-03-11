package no.nav.aap.arenaoppslag.database

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TelleverkRepositoryTest : H2TestBase("flyway/telleverk", "flyway/minimumtest") {

    @Test
    fun `hentTelleverkPåPerson returnerer korrekte verdier for person 1`() {
        val repository = TelleverkRepository(h2)
        val kvote = repository.hentTelleverkPåPerson(1)

        assertEquals(2, kvote.size)
        assertEquals(setOf(KvoteVerdi("AAP", 5280), KvoteVerdi("MAAPU", 460)), kvote)
    }

    @Test
    fun `hentTelleverkPåPerson returnerer tomt sett for ukjent person`() {
        val repository = TelleverkRepository(h2)

        val kvote = repository.hentTelleverkPåPerson(999)
        assertTrue(kvote.isEmpty())
    }

    @Test
    fun `hentTelleverkPåPerson returnerer kun data for etterspurt person`() {
        val repository = TelleverkRepository(h2)

        val kvotePerson1 = repository.hentTelleverkPåPerson(1)
        val kvotePerson2 = repository.hentTelleverkPåPerson(2)

        val aapPerson1 = kvotePerson1.find { it.kode == "AAP" }!!.verdi
        val aapPerson2 = kvotePerson2.find { it.kode == "AAP" }!!.verdi

        assertNotEquals(aapPerson1, aapPerson2)
        assertEquals(5280, aapPerson1)
        assertEquals(15060, aapPerson2)
    }



}