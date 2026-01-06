package arenaoppslag.util

import arenaoppslag.TestConfig
import arenaoppslag.datasource.ArenaDatasource
import org.flywaydb.core.Flyway
import javax.sql.DataSource

abstract class H2TestBase(
    vararg migrationLocations: String
) {

    protected val h2: DataSource = ArenaDatasource.create(
        TestConfig.oracleH2
    )

    init {
        Flyway.configure().dataSource(h2).locations("flyway/common", *migrationLocations).load().apply { migrate() }
    }

}
