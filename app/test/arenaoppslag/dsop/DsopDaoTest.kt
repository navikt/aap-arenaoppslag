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
    fun `Tester henting av vedtak`() {
        val alleVedtak = dsopDao.selectVedtak("12345678910",
            Periode(LocalDate.of(2023, 2, 2), LocalDate.of(2023, 9, 9)),
            Periode(LocalDate.of(2023, 2, 2), LocalDate.of(2023, 9, 9)))

        assertEquals(1, alleVedtak.vedtaksliste.size)
    }

}