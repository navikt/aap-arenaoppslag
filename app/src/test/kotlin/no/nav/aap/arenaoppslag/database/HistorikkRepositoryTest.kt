package no.nav.aap.arenaoppslag.database

import no.nav.aap.arenaoppslag.modeller.ArenaVedtak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class HistorikkRepositoryTest : H2TestBase("flyway/eksisterer") {

    private lateinit var historikkRepository: HistorikkRepository
    private lateinit var vedtakRepository: VedtakRepository
    private val testDato = LocalDate.parse("2025-12-15")
    // Pinnet "nå"-dato for deterministiske tester — uavhengig av faktisk systemklokke
    private val nåDato = LocalDate.parse("2026-03-01")

    @BeforeEach
    fun setUp() {
        historikkRepository = HistorikkRepository(h2)
        vedtakRepository = VedtakRepository(h2)
    }

    @Test
    fun `ingen signifikante vedtak for person som ikke finnes`() {
        val alleVedtak = historikkRepository.hentAlleSignifikanteVedtakForPerson(
            arenaPersonId = 54601 /* finnes ikke */,
            testDato,
            nåDato,
        )
        assertThat(alleVedtak).isEmpty()
    }

    @Test
    fun `ingen signifikante vedtak for person med kun veldig gamle vedtak`() {
        val testPerson = "kun_gamle"
        val testPersonId = 992
        val alleVedtak: List<ArenaVedtak> = vedtakRepository.hentVedtak(testPerson)
        assertThat(alleVedtak).hasSize(2)

        val signifikanteVedtak = historikkRepository.hentAlleSignifikanteVedtakForPerson(testPersonId, testDato, nåDato)
        assertThat(signifikanteVedtak).isEmpty()
    }

    @Test
    fun `finner signifikante vedtak for person med kun nye vedtak`() {
        val testPersonFnr = "kun_nye"
        val testPersonId = 996
        val alleVedtak = vedtakRepository.hentVedtak(testPersonFnr)
        assertThat(alleVedtak).hasSize(3)

        val signifikanteVedtak = historikkRepository.hentAlleSignifikanteVedtakForPerson(testPersonId, testDato, nåDato)
        assertThat(signifikanteVedtak).hasSize(3)
    }

    @Test
    fun `finner signifikante vedtak for person med både gamle og nye vedtak`() {
        val testPersonFnr = "blanding"
        val testPersonId = 997
        val alleVedtak = vedtakRepository.hentVedtak(testPersonFnr)
        assertThat(alleVedtak).hasSize(6)

        val signifikanteVedtak = historikkRepository.hentAlleSignifikanteVedtakForPerson(testPersonId, testDato, nåDato)
        assertThat(signifikanteVedtak).hasSize(2)
    }

    @Test
    fun `Ingen signifikante vedtak for person uten påbegynte vedtak`() {
        val testPersonFnr = "426282"
        val testPersonId = 426282
        val alleVedtak = vedtakRepository.hentVedtak(testPersonFnr)
        assertThat(alleVedtak).hasSize(1)

        val signifikanteVedtak = historikkRepository.hentAlleSignifikanteVedtakForPerson(testPersonId, testDato)
        assertThat(signifikanteVedtak).hasSize(1)
    }

}
