package arenaoppslag.dsop

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import javax.sql.DataSource

class DsopDaoTest {
    private val dataSource: DataSource
    private val flyway: Flyway
    private val dsopDao: DsopDao

    init {
        dataSource = HikariDataSource(HikariConfig().apply {
            jdbcUrl = "jdbc:h2:mem:request_no;MODE=Oracle"
            username = "sa"
            password = ""
            maximumPoolSize = 3
        })

        flyway = Flyway.configure().dataSource(dataSource).locations("flyway").load().apply { migrate() }

        dsopDao = DsopDao(dataSource)
    }

    @Test
    fun `Henter ut et enkelt vedtak`() {
        val alleVedtak = dsopDao.selectVedtak("12345678910",
            Periode(LocalDate.of(2023, 2, 2), LocalDate.of(2023, 9, 9)),
            Periode(LocalDate.of(2023, 2, 2), LocalDate.of(2023, 9, 9)))

        assertEquals(1, alleVedtak.vedtaksliste.size)
    }

    @Test
    fun `Periode settes basert på samtykkeperiode`() {
        val alleVedtak = dsopDao.selectVedtak("12345678910",
            Periode(LocalDate.of(2023, 1, 2), LocalDate.of(2023, 10, 9)),
            Periode(LocalDate.of(2023, 2, 2), LocalDate.of(2023, 9, 9)))

        val vedtak = alleVedtak.vedtaksliste.first()

        assertEquals(LocalDate.of(2023, 2, 2), vedtak.virkningsperiode.fraDato)
        assertEquals(LocalDate.of(2023, 9, 9), vedtak.virkningsperiode.tilDato)
    }

    @Test
    fun `Periode settes ikke basert på samtykkeperiode`() {
        val alleVedtak = dsopDao.selectVedtak("12345678910",
            Periode(LocalDate.of(2023, 1, 2), LocalDate.of(2023, 10, 9)),
            Periode(LocalDate.of(2022, 2, 2), LocalDate.of(2024, 9, 9)))

        val vedtak = alleVedtak.vedtaksliste.first()

        assertEquals(LocalDate.of(2023, 1, 1), vedtak.virkningsperiode.fraDato)
        assertEquals(LocalDate.of(2023, 10, 31), vedtak.virkningsperiode.tilDato)
    }

    @Test
    fun `Henter ut meldekort`() {
        val alleMeldekort = dsopDao.selectMeldekort("12345678910",
            Periode(LocalDate.of(2023, 10, 1), LocalDate.of(2023, 10, 30)),
            Periode(LocalDate.of(2023, 10, 1), LocalDate.of(2023, 9, 9)))


        assertEquals(1, alleMeldekort.meldekortliste.size)
    }

    @Test
    fun `Meldekortperiode settes basert på samtykkeperiode`() {
        val alleMeldekort = dsopDao.selectMeldekort("12345678910",
            Periode(LocalDate.of(2023, 10, 1), LocalDate.of(2023, 10, 30)),
            Periode(LocalDate.of(2023, 10, 10), LocalDate.of(2023, 10, 15)))


        val meldekort = alleMeldekort.meldekortliste.first()

        assertEquals(LocalDate.of(2023, 10, 10), meldekort.periode.fraDato)
        assertEquals(LocalDate.of(2023, 10, 15), meldekort.periode.tilDato)
    }

    @Test
    fun `Meldekortperiode settes ikke basert på samtykkeperiode`() {
        val alleMeldekort = dsopDao.selectMeldekort("12345678910",
            Periode(LocalDate.of(2023, 10, 1), LocalDate.of(2023, 10, 30)),
            Periode(LocalDate.of(2023, 9, 10), LocalDate.of(2023, 11, 15)))


        val meldekort = alleMeldekort.meldekortliste.first()

        assertEquals(LocalDate.of(2023, 10, 9), meldekort.periode.fraDato)
        assertEquals(LocalDate.of(2023, 10, 20), meldekort.periode.tilDato)
    }

    @Test
    fun `Meldekortperiode summerer hele meldekortperioden`() {
        val alleMeldekort = dsopDao.selectMeldekort("12345678910",
            Periode(LocalDate.of(2023, 10, 1), LocalDate.of(2023, 10, 30)),
            Periode(LocalDate.of(2023, 9, 10), LocalDate.of(2023, 11, 15)))


        val meldekort = alleMeldekort.meldekortliste.first()

        assertEquals(22.0, meldekort.antallTimerArbeidet)
    }

    @Test
    fun `Meldekortperiode summerer innenfor amtykkeperioden`() {
        val alleMeldekort = dsopDao.selectMeldekort("12345678910",
            Periode(LocalDate.of(2023, 10, 1), LocalDate.of(2023, 10, 30)),
            Periode(LocalDate.of(2023, 10, 13), LocalDate.of(2023, 10, 18)))


        val meldekort = alleMeldekort.meldekortliste.first()

        assertEquals(5.0, meldekort.antallTimerArbeidet)
    }

}
