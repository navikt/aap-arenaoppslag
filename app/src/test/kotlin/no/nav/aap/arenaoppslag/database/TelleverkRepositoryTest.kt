package no.nav.aap.arenaoppslag.database

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TelleverkRepositoryTest: H2TestBase("flyway/telleverk","flyway/minimumtest")  {
    @Test
    fun hentTelleverkPåPerson() {
        println("Go")
    }

}