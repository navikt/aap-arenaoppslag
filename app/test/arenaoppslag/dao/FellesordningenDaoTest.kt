package arenaoppslag.dao

import arenaoppslag.fellesordningen.FellesordningenDao
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Test
import javax.sql.DataSource
import org.junit.jupiter.api.Assertions.assertEquals
import java.time.LocalDate

class FellesordningenDaoTest {

    private val dataSource: DataSource
    private val flyway: Flyway
    private val fellesordningenDao: FellesordningenDao

    init {
        dataSource = HikariDataSource(HikariConfig().apply {
            jdbcUrl = "jdbc:h2:mem:request_no;MODE=Oracle"
            username = "sa"
            password = ""
            maximumPoolSize = 3
        })

        flyway = Flyway.configure().dataSource(dataSource).locations("flyway").load().apply { migrate() }

        fellesordningenDao = FellesordningenDao(dataSource)
    }

    @Test
    fun test() {
        val alleVedtak = fellesordningenDao.selectVedtakMedTidsbegrensning("1", LocalDate.of(2022, 10, 1))

        assertEquals(1, alleVedtak.perioder.size)
    }
}