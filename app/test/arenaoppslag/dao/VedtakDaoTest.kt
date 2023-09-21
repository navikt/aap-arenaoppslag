package arenaoppslag.dao

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Test
import javax.sql.DataSource
import org.junit.jupiter.api.Assertions.assertEquals

class VedtakDaoTest {

    private val dataSource: DataSource
    private val flyway: Flyway
    private val vedtakDao: VedtakDao

    init {
        dataSource = HikariDataSource(HikariConfig().apply {
            jdbcUrl = "jdbc:h2:mem:request_no;MODE=Oracle"
            username = "sa"
            password = ""
            maximumPoolSize = 3
        })

        flyway = Flyway.configure().dataSource(dataSource).locations("flyway").load().apply { migrate() }

        vedtakDao = VedtakDao(dataSource)
    }

    @Test
    fun `test`() {
        val alleVedtak = vedtakDao.selectAlleVedtak("1")

        assertEquals(1, alleVedtak.size)
    }
}