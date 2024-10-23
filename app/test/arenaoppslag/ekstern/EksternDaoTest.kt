package arenaoppslag.ekstern

import arenaoppslag.util.H2TestBase
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import java.time.LocalDate

class FellesordningenDaoTest : H2TestBase(
    "flyway/eksterndaotest"
) {

    @Test
    //TODO - kanskje redundant considering testen som kommer rett etter denne
    fun `hente ut gyldig minimumstruktur for enkelt vedtak`() {
        val forventetVedtaksperioder = listOf(
            Periode(LocalDate.of(2022, 8, 30), LocalDate.of(2023, 8, 30))
        )

        val alleVedtak = EksternDao.selectVedtakMinimum(
            personId = "123",
            fraOgMedDato = LocalDate.of(2022, 10, 1),
            tilOgMedDato = LocalDate.of(2023, 12, 31),
            h2.connection)

        assertEquals(VedtakResponse(forventetVedtaksperioder), alleVedtak)
    }

    @Test
    fun `hente ut gyldig minimumstruktur for flere vedtak`() {
        val forventetVedtaksperioder = listOf(
            Periode(LocalDate.of(2010, 8, 27), LocalDate.of(2018, 2, 4)),
            Periode(LocalDate.of(2019, 12, 31), LocalDate.of(2023, 1, 1))
        )

        val alleVedtak = EksternDao.selectVedtakMinimum(
            personId = "321",
            fraOgMedDato = LocalDate.of(2009, 10, 1),
            tilOgMedDato = LocalDate.of(2023, 12, 31),
            h2.connection)

        assertEquals(forventetVedtaksperioder, alleVedtak.perioder)
    }
}