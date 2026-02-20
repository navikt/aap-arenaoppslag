package no.nav.aap.arenaoppslag.database

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
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
        "Ja"
    )


    @Test
    fun `personEksistererIAapArena finner person når den skal`() {
        val finnes = setOf("12345678901")
        val finnesIkke = setOf("007")
        val personRepository = mockk<PersonRepository>()
        val historikkRepository = mockk<HistorikkRepository>() // brukes ikke

        every { personRepository.hentPersonIdHvisEksisterer(finnes) } returns 1
        every { personRepository.hentPersonIdHvisEksisterer(finnesIkke) } returns null

        val service = HistorikkService(personRepository, historikkRepository)

        val funnet = service.personEksistererIAapArena(finnes).eksisterer
        val ikkeFunnet = service.personEksistererIAapArena(finnesIkke).eksisterer

        assertThat(funnet).isEqualTo(true)
        assertThat(ikkeFunnet).isEqualTo(false)
    }


    @Test
    fun `personEksistererIAapArena bruker cachet verdi andre gang`() {
        val personIdentifikatorer = setOf("12345678901")
        val personRepository = mockk<PersonRepository>()
        val historikkRepository = mockk<HistorikkRepository>()

        every { personRepository.hentPersonIdHvisEksisterer(any()) } returns 1

        val service = HistorikkService(personRepository, historikkRepository)

        val firstCall = service.personEksistererIAapArena(personIdentifikatorer).eksisterer
        val secondCall = service.personEksistererIAapArena(personIdentifikatorer).eksisterer

        assertThat(firstCall).isTrue()
        assertThat(secondCall).isTrue()
        verify(exactly = 1) { personRepository.hentPersonIdHvisEksisterer(any()) }
    }

}
