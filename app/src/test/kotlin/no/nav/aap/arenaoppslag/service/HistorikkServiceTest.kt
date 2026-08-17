package no.nav.aap.arenaoppslag.service

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.arenaoppslag.database.HistorikkRepository
import no.nav.aap.arenaoppslag.database.PersonRepository
import no.nav.aap.arenaoppslag.modeller.ArenaVedtak
import no.nav.aap.arenaoppslag.modeller.PersonId
import org.assertj.core.api.Assertions
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
    fun `personEksistererIAapArena finner person når den skal`() {
        val finnes = setOf("12345678901")
        val finnesIkke = setOf("007")
        val personRepository = mockk<PersonRepository>()
        val historikkRepository = mockk<HistorikkRepository>() // brukes ikke

        every { personRepository.hentPersonIdHvisEksisterer(finnes) } returns PersonId(1)
        every { personRepository.hentPersonIdHvisEksisterer(finnesIkke) } returns null

        val service = HistorikkService(personRepository, historikkRepository)

        val funnet = service.personEksistererIAapArena(finnes).eksisterer
        val ikkeFunnet = service.personEksistererIAapArena(finnesIkke).eksisterer

        Assertions.assertThat(funnet).isEqualTo(true)
        Assertions.assertThat(ikkeFunnet).isEqualTo(false)
    }

    @Test
    fun `personEksistererIAapArena bruker cachet verdi andre gang`() {
        val personIdentifikatorer = setOf("12345678901")
        val personRepository = mockk<PersonRepository>()
        val historikkRepository = mockk<HistorikkRepository>()

        every { personRepository.hentPersonIdHvisEksisterer(any()) } returns PersonId(1)

        val service = HistorikkService(personRepository, historikkRepository)

        val firstCall = service.personEksistererIAapArena(personIdentifikatorer).eksisterer
        val secondCall = service.personEksistererIAapArena(personIdentifikatorer).eksisterer

        Assertions.assertThat(firstCall).isTrue()
        Assertions.assertThat(secondCall).isTrue()
        verify(exactly = 1) { personRepository.hentPersonIdHvisEksisterer(any()) }
    }

}
