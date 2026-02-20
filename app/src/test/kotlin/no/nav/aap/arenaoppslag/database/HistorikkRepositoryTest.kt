package no.nav.aap.arenaoppslag.database

import no.nav.aap.arenaoppslag.modeller.ArenaVedtak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class HistorikkRepositoryTest : H2TestBase("flyway/minimumtest", "flyway/eksisterer") {

    private lateinit var historikkRepository: HistorikkRepository
    private lateinit var vedtakRepository: VedtakRepository
    private val testDato = LocalDate.parse("2025-12-15")

    @BeforeEach
    fun setUp() {
        historikkRepository = HistorikkRepository(h2)
        vedtakRepository = VedtakRepository(h2)
    }

    @Test
    fun `ingen signifikante saker for person som ikke finnes`() {
        val alleVedtak = historikkRepository.hentAlleSignifikanteVedtakForPerson(
            personId = 54601 /* finnes ikke */,
            testDato,
        )
        assertThat(alleVedtak).isEmpty()
    }

    @Test
    fun `ingen signifikante saker for person med kun veldig gamle vedtak`() {
        val testPerson = "kun_gamle"
        val testPersonId = 992
        val alleVedtak: List<ArenaVedtak> = vedtakRepository.hentVedtak(testPerson)
        assertThat(alleVedtak).hasSize(2)

        val relevanteSaker = historikkRepository.hentAlleSignifikanteVedtakForPerson(testPersonId, testDato)
        assertThat(relevanteSaker).isEmpty()
    }

    @Test
    fun `finner signifikante saker for person med kun nye vedtak`() {
        val testPersonFnr = "kun_nye"
        val testPersonId = 996
        val alleVedtak = vedtakRepository.hentVedtak(testPersonFnr)
        assertThat(alleVedtak).hasSize(3)

        val relevanteSaker = historikkRepository.hentAlleSignifikanteVedtakForPerson(testPersonId, testDato)
        assertThat(relevanteSaker).hasSize(3)
    }

    @Test
    fun `finner signifikante saker for person med både gamle og nye vedtak`() {
        val testPersonFnr = "blanding"
        val testPersonId = 997
        val alleVedtak = vedtakRepository.hentVedtak(testPersonFnr)
        assertThat(alleVedtak).hasSize(6)

        val relevanteSaker = historikkRepository.hentAlleSignifikanteVedtakForPerson(testPersonId, testDato)
        assertThat(relevanteSaker).hasSize(2)
    }

}
