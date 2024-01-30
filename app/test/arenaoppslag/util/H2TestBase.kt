package arenaoppslag.util

import arenaoppslag.TestConfig
import arenaoppslag.datasource.Hikari
import org.flywaydb.core.Flyway
import javax.sql.DataSource

abstract class H2TestBase {

    protected val h2: DataSource = Hikari.create(
        TestConfig.oracleH2
    )

    init {
        Flyway.configure().dataSource(h2).locations("flyway").load().apply { migrate() }
    }


}