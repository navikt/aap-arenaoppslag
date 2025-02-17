package arenaoppslag.intern

import arenaoppslag.util.H2TestBase
import no.nav.aap.arenaoppslag.kontrakt.intern.SakStatus
import no.nav.aap.arenaoppslag.kontrakt.intern.Status
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class InternDaoMinimumTest : H2TestBase("flyway/minimumtest") {
    @Test
    fun `ingen vedtaks-perioder blir hentet ut for personer som ikke er tilknyttet noe vedtak`() {
        val alleVedtak = InternDao.selectVedtakMinimum(
            personId = "ingenvedtak",
            fraOgMedDato = LocalDate.of(2010, 10, 1),
            tilOgMedDato = LocalDate.of(2024, 12, 31),
            h2.connection
        )

        assertThat(alleVedtak).isEmpty()
    }

    @Test
    //TODO - kanskje redundant considering testen som kommer rett etter denne
    fun `hente ut gyldig minimumstruktur for enkelt vedtak`() {
        val forventetVedtaksperioder = listOf(
            Periode(LocalDate.of(2022, 8, 30), LocalDate.of(2023, 8, 30))
        )

        val alleVedtak = InternDao.selectVedtakMinimum(
            personId = "123",
            fraOgMedDato = LocalDate.of(2010, 10, 1),
            tilOgMedDato = LocalDate.of(2024, 12, 31),
            h2.connection
        )

        assertEquals(forventetVedtaksperioder, alleVedtak)
    }

    @Test
    fun `hente ut gyldig minimumstruktur for flere vedtak`() {
        val forventetVedtaksperioder = listOf(
            Periode(LocalDate.of(2010, 8, 27), LocalDate.of(2018, 2, 4)),
            Periode(LocalDate.of(2019, 12, 31), LocalDate.of(2023, 1, 1))
        )

        val alleVedtak = InternDao.selectVedtakMinimum(
            personId = "321",
            fraOgMedDato = LocalDate.of(2009, 10, 1),
            tilOgMedDato = LocalDate.of(2023, 12, 31),
            h2.connection
        )

        assertEquals(forventetVedtaksperioder.size, alleVedtak.size)
        assertEquals(forventetVedtaksperioder.toSet(), alleVedtak.toSet())
    }

    @Test
    fun `ikke ha med de vedtak som faller utenfor periode som blir queriet etter`() {
        val forventetVedtaksperioder = listOf(
            Periode(LocalDate.of(2019, 12, 31), LocalDate.of(2023, 1, 1))
        )

        val alleVedtak = InternDao.selectVedtakMinimum(
            personId = "321",
            fraOgMedDato = LocalDate.of(2019, 10, 1),
            tilOgMedDato = LocalDate.of(2023, 12, 31),
            h2.connection
        )

        assertEquals(forventetVedtaksperioder, alleVedtak)
    }

    @Test
    fun `ha med vedtak som overlapper med, men ikke er subset av, query-perioden`() {
        val forventetVedtaksperioder = listOf(
            Periode(LocalDate.of(2022, 8, 30), LocalDate.of(2023, 8, 30))
        )

        val alleVedtakLeftOverlap = InternDao.selectVedtakMinimum(
            personId = "123",
            fraOgMedDato = LocalDate.of(2022, 2, 1),
            tilOgMedDato = LocalDate.of(2022, 10, 31),
            h2.connection
        )

        val alleVedtakRightOverlap = InternDao.selectVedtakMinimum(
            personId = "123",
            fraOgMedDato = LocalDate.of(2023, 6, 1),
            tilOgMedDato = LocalDate.of(2024, 10, 31),
            h2.connection
        )

        assertEquals(forventetVedtaksperioder, alleVedtakLeftOverlap)
        assertEquals(forventetVedtaksperioder, alleVedtakRightOverlap)
    }

    @Test
    fun `ingen vedtaks-perioder blir hentet ut for personer som har invalid vedtak (feil VEDTAKSTATUSKODE, VEDTAKTYPEKODE, UTFALLKODE, RETTIGHETKODE)`() {
        val alleVedtak = InternDao.selectVedtakMinimum(
            personId = "invalid",
            fraOgMedDato = LocalDate.of(2010, 10, 1),
            tilOgMedDato = LocalDate.of(2024, 12, 31),
            h2.connection
        )

        assertThat(alleVedtak).isEmpty()
    }

    @Test
    fun `en person som har blanding av invalid of valid vedtak, får bare de som er valid`() {
        val forventetVedtaksperioder = listOf(
            Periode(LocalDate.of(2022, 8, 30), LocalDate.of(2023, 2, 4))
        )

        val alleVedtak = InternDao.selectVedtakMinimum(
            personId = "somevalid",
            fraOgMedDato = LocalDate.of(2010, 10, 1),
            tilOgMedDato = LocalDate.of(2024, 12, 31),
            h2.connection
        )

        assertEquals(forventetVedtaksperioder, alleVedtak)
    }

    @Test
    fun `hente vedtak med forskjellige gyldige vedtaksstatus-koder`() {
        val forventetVedtaksperioder = listOf(
            Periode(LocalDate.of(2022, 8, 27), LocalDate.of(2023, 2, 4)),
            Periode(LocalDate.of(2019, 8, 27), LocalDate.of(2023, 1, 1))
        )

        val alleVedtak = InternDao.selectVedtakMinimum(
            personId = "statuskode",
            fraOgMedDato = LocalDate.of(2010, 10, 1),
            tilOgMedDato = LocalDate.of(2024, 12, 31),
            h2.connection
        )

        assertEquals(forventetVedtaksperioder.size, alleVedtak.size)
        assertEquals(forventetVedtaksperioder.toSet(), alleVedtak.toSet())
    }

    @Test
    fun `hente vedtak med forskjellige gyldige vedtakstype-koder`() {
        val forventetVedtaksperioder = listOf(
            Periode(LocalDate.of(2022, 8, 27), LocalDate.of(2023, 2, 4)),
            Periode(LocalDate.of(2019, 8, 27), LocalDate.of(2022, 2, 4)),
            Periode(LocalDate.of(2019, 12, 31), LocalDate.of(2023, 1, 1))
        )

        val alleVedtak = InternDao.selectVedtakMinimum(
            personId = "typekode",
            fraOgMedDato = LocalDate.of(2010, 10, 1),
            tilOgMedDato = LocalDate.of(2024, 12, 31),
            h2.connection
        )

        assertEquals(forventetVedtaksperioder.size, alleVedtak.size)
        assertEquals(forventetVedtaksperioder.toSet(), alleVedtak.toSet())
    }

    @Test
    fun `hente vedtak med null til-dato`() {
        val forventetVedtaksperioder = listOf(
            Periode(LocalDate.of(2022, 8, 30), null)
        )

        val alleVedtak = InternDao.selectVedtakMinimum(
            personId = "nulltildato",
            fraOgMedDato = LocalDate.of(2010, 10, 1),
            tilOgMedDato = LocalDate.of(2024, 12, 31),
            h2.connection
        )

        assertEquals(forventetVedtaksperioder, alleVedtak)
    }

    @Test
    fun `hente aktfasePerioder`() {
        val forventetVedtaksperioder = listOf(
            SakStatus(sakId = "0",Status.IVERK,no.nav.aap.arenaoppslag.kontrakt.modeller.Periode(LocalDate.of(2022, 8, 30), null))
        )

        val alleVedtak = InternDao.selectSaker(
            personidentifikator = "nulltildato",
            h2.connection
        )

        assertEquals(forventetVedtaksperioder, alleVedtak)
    }
}