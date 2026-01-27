package arenaoppslag.database

import no.nav.aap.arenaoppslag.database.PersonRepository
import no.nav.aap.arenaoppslag.database.PersonRepository.Companion.historiskeRettighetkoderIArena
import no.nav.aap.arenaoppslag.database.SakRepository
import no.nav.aap.arenaoppslag.kontrakt.intern.ArenaSak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PersonRepositoryTest : H2TestBase("flyway/minimumtest", "flyway/eksisterer") {

    private lateinit var personRepository: PersonRepository
    private lateinit var sakRepository: SakRepository
    private val testDato = LocalDate.parse("2025-12-15")

    @BeforeEach
    fun setUp() {
        personRepository = PersonRepository(h2)
        sakRepository = SakRepository(h2)
    }

    @Test
    fun `ingen saker for person som ikke finnes`() {
        val alleVedtak = personRepository.hentAlleSignifikanteSakerForPerson(
            personId = 54601 /* finnes ikke */,
            testDato,
        )
        assertThat(alleVedtak).isEmpty()
    }

    @Test
    fun `ingen saker for person med kun vedtak på historiske rettighetkoder`() {
        val testPerson = "kun_gamle"
        val testPersonId = 992
        val alleSaker:List<ArenaSak> = sakRepository.hentSaker(testPerson)
        val kunHistoriske =
            alleSaker.map { it.rettighetkode }.filter { it in historiskeRettighetkoderIArena }
        assertThat(kunHistoriske).hasSize(2)

        val relevanteSaker = personRepository.hentAlleSignifikanteSakerForPerson(testPersonId, testDato)
        assertThat(relevanteSaker).isEmpty()
    }

    @Test
    fun `finner saker for person med vedtak på kun nye rettighetkoder`() {
        val testPersonFnr = "kun_nye"
        val testPersonId = 996
        val alleSaker = sakRepository.hentSaker(testPersonFnr)
        val kunHistoriske =
            alleSaker.map { it.rettighetkode }.filter { it in historiskeRettighetkoderIArena }
        assertThat(kunHistoriske).isEmpty() // ingen historiske koder

        val relevanteSaker = personRepository.hentAlleSignifikanteSakerForPerson(testPersonId, testDato)
        assertThat(relevanteSaker).hasSize(2)
    }

    @Test
    fun `finner saker for person med vedtak på både nye og historiske rettighetkoder`() {
        val testPersonFnr = "blanding"
        val testPersonId = 997
        val alleSaker = sakRepository.hentSaker(testPersonFnr)
        val kunHistoriske =
            alleSaker.map { it.rettighetkode }.filter { it in historiskeRettighetkoderIArena }
        assertThat(kunHistoriske).hasSize(2) // noen historiske koder

        val relevanteSaker = personRepository.hentAlleSignifikanteSakerForPerson(testPersonId, testDato)
        assertThat(relevanteSaker).hasSize(1)
    }

    @Test
    fun `kombinert spørring for relevant historikk kjører uten feil`(){
        val testPersonId = 997
        val relevanteSaker = personRepository.hentAlleSignifikanteSakerForPerson(testPersonId, testDato)
        assertThat(relevanteSaker).hasSize(1)
    }

    // Gammel regel for å sjekke om person finnes i Arena
    @Test
    fun `returnerer true om person eksisterer`() {
        val fodselsnr = "1"
        val personEksisterer = personRepository.hentEksistererIAAPArena(fodselsnr)
        assertEquals(true, personEksisterer)
        val personEksistererIkke = personRepository.hentEksistererIAAPArena("2012012031")
        assertEquals(false, personEksistererIkke)
    }

}
