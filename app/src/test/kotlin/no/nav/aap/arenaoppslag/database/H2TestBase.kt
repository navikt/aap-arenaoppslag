package no.nav.aap.arenaoppslag.database

import no.nav.aap.arenaoppslag.DbConfig
import org.flywaydb.core.Flyway
import javax.sql.DataSource

abstract class H2TestBase(vararg migrationLocations: String) {

    private val dbName = this::class.simpleName ?: "testdb_${System.nanoTime()}"

    private val dbConfig = DbConfig(
        username = "SA",
        password = "",
        url = "jdbc:h2:mem:$dbName;MODE=Oracle;DB_CLOSE_DELAY=-1",
        driver = "org.h2.Driver"
    )

    protected val h2: DataSource = ArenaDatasource.create(dbConfig)

    init {
        Flyway.configure().dataSource(h2).locations("flyway/common", *migrationLocations).load().apply { migrate() }
    }

}


