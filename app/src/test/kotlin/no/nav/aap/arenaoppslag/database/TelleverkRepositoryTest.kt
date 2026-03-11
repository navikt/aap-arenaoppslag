package no.nav.aap.arenaoppslag.database

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TelleverkRepositoryTest : H2TestBase("flyway/telleverk", "flyway/minimumtest", "flyway/eksisterer") {


    @Test
    fun `hentTelleverkPåPerson returnerer korrekte verdier for person 1`() {

        val telleverkRepository = TelleverkRepository(h2)

        val kvote = telleverkRepository.hentTelleverkPåPerson("123")

        assertEquals(2, kvote.size)
        assertEquals(setOf(KvoteVerdi("AAP", 5280), KvoteVerdi("MAAPU", 460)), kvote)
    }

    @Test
    fun `hentTelleverkPåPerson returnerer tomt sett for ukjent person`() {
        val telleverkRepository = TelleverkRepository(h2)

        val kvote = telleverkRepository.hentTelleverkPåPerson("000")
        assertTrue(kvote.isEmpty())
    }

    @Test
    fun `hentTelleverkPåPerson returnerer kun data for etterspurt person`() {
        val telleverkRepository = TelleverkRepository(h2)

        val kvotePerson1 = telleverkRepository.hentTelleverkPåPerson("123")
        val kvotePerson2 = telleverkRepository.hentTelleverkPåPerson("321")

        val aapPerson1 = kvotePerson1.find { it.kode == "AAP" }!!.verdi
        val aapPerson2 = kvotePerson2.find { it.kode == "AAP" }!!.verdi

        assertNotEquals(aapPerson1, aapPerson2)
        assertEquals(5280, aapPerson1)
        assertEquals(15060, aapPerson2)
    }

}