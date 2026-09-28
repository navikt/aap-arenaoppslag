package no.nav.aap.arenaoppslag

import io.ktor.http.HttpStatusCode
import no.nav.aap.arenaoppslag.client.ArenaOppslagGateway.Companion.withTestServer
import no.nav.aap.arenaoppslag.database.H2TestBase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

// Telleverk (saldo, maksdato, siste utbetaling) eksponeres av /api/intern/sak/{sakid}/telleverk.
class TelleverkApiTest : H2TestBase("flyway/maksimum") {

    @Test
    fun `responsen inneholder telleverk for personen bak saken`() {
        withTestServer(h2) { gateway ->
            val response = gateway.hentTelleverk("2023-9001")

            // Saldoen for personen som helhet kommer fra BEREGNINGSLEDD og ligger på telleverk,
            // ikke på tilkjent ytelse.
            assertThat(response.telleverk?.ordineerAAPKvote).isEqualTo(4)
            assertThat(response.telleverk?.utvidetAAPKvote).isEqualTo(25)
        }
    }

    @Test
    fun `ugyldig saksnummerformat gir 400`() {
        withTestServer(h2) { gateway ->
            assertThat(gateway.hentTelleverkStatus("99999")).isEqualTo(HttpStatusCode.BadRequest)
        }
    }

    @Test
    fun `ukjent sak gir 404`() {
        withTestServer(h2) { gateway ->
            assertThat(gateway.hentTelleverkStatus("2023-99999")).isEqualTo(HttpStatusCode.NotFound)
        }
    }
}
