package arenaoppslag.intern

import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.aap.arenaoppslag.kontrakt.intern.ArenaSak
import no.nav.aap.arenaoppslag.kontrakt.modeller.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import javax.sql.DataSource
import kotlin.random.Random

@ExtendWith(MockKExtension::class)
class ArenaRepositoryTest {

    @MockK(relaxed = true)
    private lateinit var dataSource: DataSource

    private lateinit var underTest: ArenaRepository

    @BeforeEach
    fun setUp() {
        underTest = ArenaRepository(dataSource)
    }

    @Test
    fun `Sortering av relevante saker tar tom liste`() {
        val nyeste = underTest.finnNyesteSakId(emptyList())
        assertThat(nyeste).isNull()
    }

    @Test
    fun `Sortering av relevante saker tar liste med datoer`() {
        var teller = 1
        val nyeste = underTest.finnNyesteSakId(
            listOf(
                testSak(teller++, LocalDate.now().plusYears(1)),
                testSak(teller++, LocalDate.now().plusYears(3)),
                testSak(teller, LocalDate.now().plusYears(2)),
                )
        )
        assertThat(nyeste).isEqualTo("2")
    }

    @Test
    fun `Sortering av relevante saker prioriterer null`() {
        var teller = 1
        val nyeste = underTest.finnNyesteSakId(
            listOf(
                testSak(teller++, LocalDate.now().plusYears(1)),
                testSak(teller++, null),
                testSak(teller, LocalDate.now().plusYears(2)),
                )
        )
        assertThat(nyeste).isEqualTo("2")
    }

    private fun testSak(sakId: Int, tilOgMedDato: LocalDate?) = ArenaSak(
        sakId.toString(),
        "AKTIV",
        Periode(LocalDate.now().minusYears(5), tilOgMedDato),
        "AAP"
    )

}