package no.nav.aap.arenaoppslag.service

import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.aap.arenaoppslag.database.MaksimumRepository
import no.nav.aap.arenaoppslag.database.PeriodeRepository
import no.nav.aap.arenaoppslag.database.PersonRepository
import no.nav.aap.arenaoppslag.database.TelleverkRepository
import no.nav.aap.arenaoppslag.database.VedtakRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import javax.sql.DataSource

@ExtendWith(MockKExtension::class)
class InternServiceTest {

    @MockK(relaxed = true)
    private lateinit var datasource: DataSource

    private lateinit var underTest: InternService

    @BeforeEach
    fun setUp() {
        val maksimumRepository = MaksimumRepository(datasource)
        val periodeRepository = PeriodeRepository(datasource)
        val vedtakRepository = VedtakRepository(datasource)
        val telleverkRepository = TelleverkRepository(datasource)
        val personRepository = PersonRepository(datasource)
        underTest = InternService(maksimumRepository, periodeRepository, vedtakRepository)
    }

    @Test
    fun `kan kalle på maksimum`() {
        underTest.hentMaksimum("ff", LocalDate.now().minusDays(1), LocalDate.now())
    }

    @Test
    fun `kan kalle på 11-17 perioder`() {
        underTest.hent11_17Perioder("ff", LocalDate.now().minusDays(1), LocalDate.now())
    }

    @Test
    fun `kan kalle på perioder`() {
        underTest.hentPerioder("ff", LocalDate.now().minusDays(1), LocalDate.now())
    }

    @Test
    fun `kan kalle på saker`() {
        underTest.hentSaker(setOf("ff"))
    }

}