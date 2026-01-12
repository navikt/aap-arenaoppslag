package arenaoppslag.database

import arenaoppslag.aap.database.PersonRepository
import arenaoppslag.aap.database.PersonRepository.Companion.historiskeRettighetkoderIArena
import arenaoppslag.aap.database.SakRepository
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
        val alleVedtak = personRepository.hentRelevanteArenaSaker(
            listOf("finnes_ikke"),
            testDato,
        )
        assertThat(alleVedtak).isEmpty()
    }

    @Test
    fun `ingen saker for person med kun vedtak på historiske rettighetkoder`() {
        val testPerson = listOf("kun_gamle")
        val alleSaker:List<ArenaSak> = testPerson.flatMap { sakRepository.hentSaker(it) }
        val kunHistoriske =
            alleSaker.map { it.rettighetkode }.filter { it in historiskeRettighetkoderIArena }
        assertThat(kunHistoriske).hasSize(2)

        val relevanteSaker = personRepository.hentRelevanteArenaSaker(testPerson, testDato)
        assertThat(relevanteSaker).isEmpty()
    }

    @Test
    fun `finner saker for person med vedtak på kun nye rettighetkoder`() {
        val testPerson = listOf("kun_nye")
        val alleSaker = testPerson.flatMap { sakRepository.hentSaker(it) }
        val kunHistoriske =
            alleSaker.map { it.rettighetkode }.filter { it in historiskeRettighetkoderIArena }
        assertThat(kunHistoriske).isEmpty() // ingen historiske koder

        val relevanteSaker = personRepository.hentRelevanteArenaSaker(testPerson, testDato)
        assertThat(relevanteSaker).hasSize(2)
    }

    @Test
    fun `finner saker for person med vedtak på både nye og historiske rettighetkoder`() {
        val testPerson = listOf("blanding")
        val alleSaker = testPerson.flatMap { sakRepository.hentSaker(it) }
        val kunHistoriske =
            alleSaker.map { it.rettighetkode }.filter { it in historiskeRettighetkoderIArena }
        assertThat(kunHistoriske).hasSize(2) // noen historiske koder

        val relevanteSaker = personRepository.hentRelevanteArenaSaker(testPerson, testDato)
        assertThat(relevanteSaker).hasSize(1)
    }

    @Test
    fun `kombinert spørring for relevant historikk kjører uten feil`(){
        val testPerson = listOf("blanding")
        val relevanteSaker = personRepository.hentAlleRelevanteSaker(testPerson, testDato)
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
