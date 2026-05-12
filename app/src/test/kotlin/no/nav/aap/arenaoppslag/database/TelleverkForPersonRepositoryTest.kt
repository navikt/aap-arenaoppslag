package no.nav.aap.arenaoppslag.database

import no.nav.aap.arenaoppslag.modeller.PersonId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test


class TelleverkForPersonRepositoryTest : H2TestBase("flyway/telleverk", "flyway/minimumtest", "flyway/eksisterer") {


    @Test
    fun `hentTelleverkPåPerson returnerer korrekte verdier for person 1`() {

        val telleverkRepository = TelleverkRepository(h2)

        val kvote = telleverkRepository.hentTelleverkPåPerson(PersonId(1))

        assertThat(kvote).hasSize(2)
        assertThat(kvote).isEqualTo(setOf(KvoteVerdi("AAP", 5280), KvoteVerdi("MAAPU", 460)))
    }

    @Test
    fun `hentTelleverkPåPerson returnerer tomt sett for ukjent person`() {
        val telleverkRepository = TelleverkRepository(h2)

        val kvote = telleverkRepository.hentTelleverkPåPerson(PersonId(999))
        assertThat(kvote).isEmpty()
    }

    @Test
    fun `hentTelleverkPåPerson returnerer kun data for etterspurt person`() {
        val telleverkRepository = TelleverkRepository(h2)

        val kvotePerson1 = telleverkRepository.hentTelleverkPåPerson(PersonId(1))
        val kvotePerson2 = telleverkRepository.hentTelleverkPåPerson(PersonId(2))

        val aapPerson1 = kvotePerson1.find { it.kode == "AAP" }!!.verdi
        val aapPerson2 = kvotePerson2.find { it.kode == "AAP" }!!.verdi

        assertThat(aapPerson1).isNotEqualTo(aapPerson2)
        assertThat(aapPerson1).isEqualTo(5280)
        assertThat(aapPerson2).isEqualTo(15060)
    }

}
