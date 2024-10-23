package arenaoppslag.ekstern

import arenaoppslag.util.H2TestBase
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import java.time.LocalDate
import kotlin.test.assertContains

class EksternDaoMinimumTest : H2TestBase(
    "flyway/eksterndaotest"
) {
    @Test
    fun `ingen vedtaks-perioder blir hentet ut for personer som ikke er tilknyttet noe vedtak`() {
        val alleVedtak = EksternDao.selectVedtakMinimum(
            personId = "ingenvedtak",
            fraOgMedDato = LocalDate.of(2022, 10, 1),
            tilOgMedDato = LocalDate.of(2023, 12, 31),
            h2.connection
        )

        assertEquals(VedtakResponse(listOf()), alleVedtak)
    }

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
            h2.connection
        )

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
            h2.connection
        )

        assertEquals(forventetVedtaksperioder.size, alleVedtak.perioder.size)
        assertEquals(forventetVedtaksperioder.toSet(), alleVedtak.perioder.toSet())
    }

    @Test
    fun `ikke ha med de vedtak som faller utenfor periode som blir queriet etter`() {
        val forventetVedtaksperioder = listOf(
            Periode(LocalDate.of(2019, 12, 31), LocalDate.of(2023, 1, 1))
        )

        val alleVedtak = EksternDao.selectVedtakMinimum(
            personId = "321",
            fraOgMedDato = LocalDate.of(2019, 10, 1),
            tilOgMedDato = LocalDate.of(2023, 12, 31),
            h2.connection
        )

        assertEquals(VedtakResponse(forventetVedtaksperioder), alleVedtak)
    }

    @Test
    fun `ha med vedtak som overlapper med, men ikke er subset av, query-perioden`() {
        val forventetVedtaksperioder = listOf(
            Periode(LocalDate.of(2022, 8, 30), LocalDate.of(2023, 8, 30))
        )

        val alleVedtakLeftOverlap = EksternDao.selectVedtakMinimum(
            personId = "123",
            fraOgMedDato = LocalDate.of(2022, 2, 1),
            tilOgMedDato = LocalDate.of(2022, 10, 31),
            h2.connection
        )

        val alleVedtakRightOverlap = EksternDao.selectVedtakMinimum(
            personId = "123",
            fraOgMedDato = LocalDate.of(2023, 6, 1),
            tilOgMedDato = LocalDate.of(2024, 10, 31),
            h2.connection
        )

        assertEquals(VedtakResponse(forventetVedtaksperioder), alleVedtakLeftOverlap)
        assertEquals(VedtakResponse(forventetVedtaksperioder), alleVedtakRightOverlap)
    }
}