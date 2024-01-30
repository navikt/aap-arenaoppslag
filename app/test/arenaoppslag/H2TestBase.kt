package arenaoppslag

import arenaoppslag.datasource.Hikari
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.BeforeEach
import javax.sql.DataSource

abstract class H2TestBase {

    protected val h2: DataSource = Hikari.create(
        TestConfig.postgres
    )

    init {
        Flyway.configure().dataSource(h2).locations("flyway").load().apply { migrate() }
    }


}