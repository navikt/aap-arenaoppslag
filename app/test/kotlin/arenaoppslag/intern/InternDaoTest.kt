package arenaoppslag.intern

import arenaoppslag.util.H2TestBase
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import java.time.LocalDate

class FellesordningenDaoTest : H2TestBase() {

    @Test
    fun test() {
        val alleVedtak = InternDao.selectVedtakMinimum(
            personId = "1",
            fraOgMedDato = LocalDate.of(2022, 10, 1),
            tilOgMedDato = LocalDate.of(2023, 12, 31),
            h2.connection)

        assertEquals(1, alleVedtak.size)
    }
}