package arenaoppslag.intern

import arenaoppslag.intern.InternDao.historiskeRettighetkoderIArena
import arenaoppslag.util.H2TestBase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ArenaHistorikkTest : H2TestBase("flyway/minimumtest", "flyway/eksisterer") {

    private val testDato = LocalDate.parse("2025-12-15")

    @Test
    fun `ingen saker for person som ikke finnes`() {
        val alleVedtak = InternDao.selectPersonMedRelevantHistorikk(
            "finnes_ikke",
            testDato,
            h2.connection
        )
        assertThat(alleVedtak).isEmpty()
    }

    @Test
    fun `ingen saker for person med kun vedtak på historiske rettighetkoder`() {
        val testPerson = "kun_gamle"
        val alleSaker = InternDao.selectAlleSaker(testPerson, h2.connection)
        val kunHistoriske = alleSaker.map { it.rettighetkode }.filter { it in historiskeRettighetkoderIArena }
        assertThat(kunHistoriske).hasSize(2)

        val relevanteSaker = InternDao.selectPersonMedRelevantHistorikk(testPerson, testDato, h2.connection)
        assertThat(relevanteSaker).isEmpty()
    }

    @Test
    fun `finner saker for person med vedtak på kun nye rettighetkoder`() {
        val testPerson = "kun_nye"
        val alleSaker = InternDao.selectAlleSaker(testPerson, h2.connection)
        val kunHistoriske = alleSaker.map { it.rettighetkode }.filter { it in historiskeRettighetkoderIArena }
        assertThat(kunHistoriske).isEmpty() // ingen historiske koder

        val relevanteSaker = InternDao.selectPersonMedRelevantHistorikk(testPerson, testDato, h2.connection)
        assertThat(relevanteSaker).hasSize(3)
    }

    @Test
    fun `finner saker for person med vedtak på både nye og historiske rettighetkoder`() {
        val testPerson = "blanding"
        val alleSaker = InternDao.selectAlleSaker(testPerson, h2.connection)
        val kunHistoriske = alleSaker.map { it.rettighetkode }.filter { it in historiskeRettighetkoderIArena }
        assertThat(kunHistoriske).hasSize(2) // noen historiske koder

        val relevanteSaker = InternDao.selectPersonMedRelevantHistorikk(testPerson, testDato, h2.connection)
        assertThat(relevanteSaker).hasSize(1)
    }

    // Gammel regel for å sjekke om person finnes i Arena
    @Test
    fun `returnerer true om person eksisterer`() {
        val personId = "1"
        val personEksisterer = InternDao.selectPersonMedFnrEksisterer(personId, h2.connection)
        assertEquals(true, personEksisterer)
        val personEksistererIkke = InternDao.selectPersonMedFnrEksisterer("2", h2.connection)
        assertEquals(false, personEksistererIkke)
    }

}