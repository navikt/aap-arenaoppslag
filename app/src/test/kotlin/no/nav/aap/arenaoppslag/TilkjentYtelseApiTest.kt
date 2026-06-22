package no.nav.aap.arenaoppslag

import no.nav.aap.arenaoppslag.client.ArenaOppslagGateway.Companion.withTestServer
import no.nav.aap.arenaoppslag.database.H2TestBase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TilkjentYtelseApiTest : H2TestBase("flyway/maksimum") {

    @Test
    fun `henter tilkjent ytelse med meldekort for sak`() {
        withTestServer(h2) { gateway ->
            val response = gateway.hentTilkjenteYtelserForSak(9001)

            assertThat(response.sakId).isEqualTo(9001)
            assertThat(response.rader).hasSize(2)

            val rad = response.rader.first { it.meldekort?.meldekortId == 5001L }
            assertThat(rad.kilde).isEqualTo("Meldekort")
            assertThat(rad.uke).isEqualTo("1-2")
            assertThat(rad.beregnetBrutto).isEqualTo(7700)
            assertThat(rad.fraOgMedDato).isEqualTo(LocalDate.of(2023, 1, 2))
            assertThat(rad.meldekort?.uker).isNotEmpty()
        }
    }

    @Test
    fun `ukjent sak gir tomt resultat`() {
        withTestServer(h2) { gateway ->
            val response = gateway.hentTilkjenteYtelserForSak(99999)

            assertThat(response.rader).isEmpty()
            assertThat(response.gjenstaaendeOrdinaerDager).isNull()
        }
    }
}

