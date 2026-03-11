package no.nav.aap.arenaoppslag.database

import no.nav.aap.arenaoppslag.modeller.ArenaSak
import no.nav.aap.arenaoppslag.modeller.ArenaSakPerson
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SakRepositoryTest : H2TestBase("flyway/minimumtest") {

    @Test
    fun `klarer å hente en sak fra databasen`() {
        val forvantetSak = ArenaSak(
            sakId = "1",
            opprettetAar = 2021,
            lopenr = 1,
            statuskode = "INAKT",
            statusnavn = "Inaktiv",
            registrertDato = LocalDate.of(2022, 2, 2).atStartOfDay(),
            avsluttetDato = null,
            person = ArenaSakPerson(
                personId = 1,
                fodselsnummer = "123",
                fornavn = "Test",
                etternavn = "Bestesen",
            )
        )

        val sakRepository = SakRepository(h2)

        val sak = sakRepository.hentSak(1)

        assertThat(sak).isEqualTo(forvantetSak)
    }

    @Test
    fun `returnerer NULL om saken ikke finnes i databasen`() {
        val sakRepository = SakRepository(h2)
        val sak = sakRepository.hentSak(1919191919)
        assertThat(sak).isNull()
    }
}
