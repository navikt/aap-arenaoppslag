package arenaoppslag.database

import arenaoppslag.TestConfig
import org.flywaydb.core.Flyway
import javax.sql.DataSource

abstract class H2TestBase(vararg migrationLocations: String) {

    protected val h2: DataSource = ArenaDatasource.create(TestConfig.oracleH2)

    init {
        Flyway.configure().dataSource(h2).locations("flyway/common", *migrationLocations).load().apply { migrate() }
    }

}
