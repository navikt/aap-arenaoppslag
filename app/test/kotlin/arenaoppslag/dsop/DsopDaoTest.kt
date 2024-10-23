package arenaoppslag.dsop

import arenaoppslag.util.H2TestBase
import no.nav.aap.arenaoppslag.kontrakt.dsop.Periode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DsopDaoTest : H2TestBase() {
    @Test
    fun `Henter ut et enkelt vedtak`() {
        val alleVedtak = DsopDao.selectVedtak("12345678910",
            Periode(LocalDate.of(2023, 2, 2), LocalDate.of(2023, 9, 9)),
            Periode(LocalDate.of(2023, 2, 2), LocalDate.of(2023, 9, 9)),
            h2.connection)

        assertEquals(1, alleVedtak.vedtaksliste.size)
    }

    @Test
    fun `Periode settes basert på samtykkeperiode`() {
        val alleVedtak = DsopDao.selectVedtak("12345678910",
            Periode(LocalDate.of(2023, 1, 2), LocalDate.of(2023, 10, 9)),
            Periode(LocalDate.of(2023, 2, 2), LocalDate.of(2023, 9, 9)),
            h2.connection)

        val vedtak = alleVedtak.vedtaksliste.first()

        assertEquals(LocalDate.of(2023, 2, 2), vedtak.virkningsperiode.fraDato)
        assertEquals(LocalDate.of(2023, 9, 9), vedtak.virkningsperiode.tilDato)
    }

    @Test
    fun `Periode settes ikke basert på samtykkeperiode`() {
        val alleVedtak = DsopDao.selectVedtak("12345678910",
            Periode(LocalDate.of(2023, 1, 2), LocalDate.of(2023, 10, 9)),
            Periode(LocalDate.of(2022, 2, 2), LocalDate.of(2024, 9, 9)),
            h2.connection)

        val vedtak = alleVedtak.vedtaksliste.first()

        assertEquals(LocalDate.of(2023, 1, 1), vedtak.virkningsperiode.fraDato)
        assertEquals(LocalDate.of(2023, 10, 31), vedtak.virkningsperiode.tilDato)
    }

    @Test
    fun `Henter ut meldekort`() {
        val alleMeldekort = DsopDao.selectMeldekort("12345678910",
            Periode(LocalDate.of(2023, 10, 1), LocalDate.of(2023, 10, 30)),
            Periode(LocalDate.of(2023, 10, 1), LocalDate.of(2023, 9, 9)),
            h2.connection)

        assertEquals(1, alleMeldekort.meldekortliste.size)
    }

    @Test
    fun `Meldekortperiode settes basert på samtykkeperiode`() {
        val alleMeldekort = DsopDao.selectMeldekort("12345678910",
            Periode(LocalDate.of(2023, 10, 1), LocalDate.of(2023, 10, 30)),
            Periode(LocalDate.of(2023, 10, 10), LocalDate.of(2023, 10, 15)),
            h2.connection)

        val meldekort = alleMeldekort.meldekortliste.first()

        assertEquals(LocalDate.of(2023, 10, 10), meldekort.periode.fraDato)
        assertEquals(LocalDate.of(2023, 10, 15), meldekort.periode.tilDato)
    }

    @Test
    fun `Meldekortperiode settes ikke basert på samtykkeperiode`() {
        val alleMeldekort = DsopDao.selectMeldekort("12345678910",
            Periode(LocalDate.of(2023, 10, 1), LocalDate.of(2023, 10, 30)),
            Periode(LocalDate.of(2023, 9, 10), LocalDate.of(2023, 11, 15)),
            h2.connection)

        val meldekort = alleMeldekort.meldekortliste.first()

        assertEquals(LocalDate.of(2023, 10, 9), meldekort.periode.fraDato)
        assertEquals(LocalDate.of(2023, 10, 20), meldekort.periode.tilDato)
    }

    @Test
    fun `Meldekortperiode summerer hele meldekortperioden`() {
        val alleMeldekort = DsopDao.selectMeldekort("12345678910",
            Periode(LocalDate.of(2023, 10, 1), LocalDate.of(2023, 10, 30)),
            Periode(LocalDate.of(2023, 9, 10), LocalDate.of(2023, 11, 15)),
            h2.connection)

        val meldekort = alleMeldekort.meldekortliste.first()

        assertEquals(22.0, meldekort.antallTimerArbeidet)
    }

    @Test
    fun `Meldekortperiode summerer innenfor amtykkeperioden`() {
        val alleMeldekort = DsopDao.selectMeldekort("12345678910",
            Periode(LocalDate.of(2023, 10, 1), LocalDate.of(2023, 10, 30)),
            Periode(LocalDate.of(2023, 10, 13), LocalDate.of(2023, 10, 18)),
            h2.connection)

        val meldekort = alleMeldekort.meldekortliste.first()

        assertEquals(5.0, meldekort.antallTimerArbeidet)
    }
}
