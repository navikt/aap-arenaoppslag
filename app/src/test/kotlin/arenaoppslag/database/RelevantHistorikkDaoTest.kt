package arenaoppslag.database

import arenaoppslag.database.RelevantHistorikkDao.historiskeRettighetkoderIArena
import arenaoppslag.database.H2TestBase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class RelevantHistorikkDaoTest : H2TestBase("flyway/minimumtest", "flyway/eksisterer") {

    private val testDato = LocalDate.parse("2025-12-15")

    @Test
    fun `ingen saker for person som ikke finnes`() {
        val alleVedtak = RelevantHistorikkDao.selectPersonMedRelevantHistorikk(
            listOf("finnes_ikke"),
            testDato,
            h2.connection
        )
        assertThat(alleVedtak).isEmpty()
    }

    @Test
    fun `ingen saker for person med kun vedtak på historiske rettighetkoder`() {
        val testPerson = listOf("kun_gamle")
        val alleSaker = testPerson.flatMap { RelevantHistorikkDao.selectAlleSaker(it, h2.connection) }
        val kunHistoriske = alleSaker.map { it.rettighetkode }.filter { it in historiskeRettighetkoderIArena }
        assertThat(kunHistoriske).hasSize(2)

        val relevanteSaker = RelevantHistorikkDao.selectPersonMedRelevantHistorikk(testPerson, testDato, h2.connection)
        assertThat(relevanteSaker).isEmpty()
    }

    @Test
    fun `finner saker for person med vedtak på kun nye rettighetkoder`() {
        val testPerson = listOf("kun_nye")
        val alleSaker = testPerson.flatMap { RelevantHistorikkDao.selectAlleSaker(it, h2.connection) }
        val kunHistoriske = alleSaker.map { it.rettighetkode }.filter { it in historiskeRettighetkoderIArena }
        assertThat(kunHistoriske).isEmpty() // ingen historiske koder

        val relevanteSaker = RelevantHistorikkDao.selectPersonMedRelevantHistorikk(testPerson, testDato, h2.connection)
        assertThat(relevanteSaker).hasSize(2)
    }

    @Test
    fun `finner saker for person med vedtak på både nye og historiske rettighetkoder`() {
        val testPerson = listOf("blanding")
        val alleSaker = testPerson.flatMap { RelevantHistorikkDao.selectAlleSaker(it, h2.connection) }
        val kunHistoriske = alleSaker.map { it.rettighetkode }.filter { it in historiskeRettighetkoderIArena }
        assertThat(kunHistoriske).hasSize(2) // noen historiske koder

        val relevanteSaker = RelevantHistorikkDao.selectPersonMedRelevantHistorikk(testPerson, testDato, h2.connection)
        assertThat(relevanteSaker).hasSize(1)
    }

    // Gammel regel for å sjekke om person finnes i Arena
    @Test
    fun `returnerer true om person eksisterer`() {
        val fodselsnr = "1"
        val personEksisterer = RelevantHistorikkDao.selectPersonMedFnrEksisterer(fodselsnr, h2.connection)
        assertEquals(true, personEksisterer)
        val personEksistererIkke = RelevantHistorikkDao.selectPersonMedFnrEksisterer("2012012031", h2.connection)
        assertEquals(false, personEksistererIkke)
    }

}
