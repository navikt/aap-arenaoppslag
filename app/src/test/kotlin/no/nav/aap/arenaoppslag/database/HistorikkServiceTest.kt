package no.nav.aap.arenaoppslag.database

import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.aap.arenaoppslag.HistorikkService
import no.nav.aap.arenaoppslag.modeller.ArenaVedtak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import javax.sql.DataSource

@ExtendWith(MockKExtension::class)
class HistorikkServiceTest {

    @MockK(relaxed = true)
    private lateinit var datasource: DataSource

    private lateinit var underTest: HistorikkService

    @BeforeEach
    fun setUp() {
        val personRepository = PersonRepository(datasource)
        val historikkRepository = HistorikkRepository(datasource)
        underTest = HistorikkService(personRepository, historikkRepository)
    }

    @Test
    fun `Sortering av signifikante vedtak tar tom liste`() {
        val nyeste = underTest.sorterVedtak(emptyList())
        assertThat(nyeste).isEmpty()
    }

    @Test
    fun `Sortering av signifikante vedtak tar liste med datoer`() {
        var teller = 1
        val nyeste = underTest.sorterVedtak(
            listOf(
                testVedtak(teller++, LocalDate.now().plusYears(1)),
                testVedtak(teller++, LocalDate.now().plusYears(3)),
                testVedtak(teller, LocalDate.now().plusYears(2)),
            )
        )
        assertThat(nyeste.map { it.sakId }).isEqualTo(listOf("2", "3", "1"))
    }

    @Test
    fun `Sortering av signifikante vedtak prioriterer null`() {
        var teller = 1
        val nyeste = underTest.sorterVedtak(
            listOf(
                testVedtak(teller++, LocalDate.now().plusYears(1)),
                testVedtak(teller++, null),
                testVedtak(teller, LocalDate.now().plusYears(2)),
            )
        )
        assertThat(nyeste.map { it.sakId }).isEqualTo(listOf("2", "3", "1"))
    }

    private fun testVedtak(sakId: Int, tilDato: LocalDate?) = ArenaVedtak(
        sakId.toString(),
        "O",
        "AKTIV",
        LocalDate.now().minusYears(5),
        tilDato = tilDato,
        "AAP",
    )

}
