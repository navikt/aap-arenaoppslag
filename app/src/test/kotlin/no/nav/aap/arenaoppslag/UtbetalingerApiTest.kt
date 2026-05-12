package no.nav.aap.arenaoppslag

import no.nav.aap.arenaoppslag.client.ArenaOppslagGateway.Companion.withTestServer
import no.nav.aap.arenaoppslag.database.H2TestBase
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SisteUtbetalingerRequest
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SisteUtbetalingerResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class UtbetalingerApiTest : H2TestBase("flyway/postering") {

    @Test
    fun `Henter ut siste utbetaling by sakIdListe, ukjent personId`() {
        withTestServer(h2) { gateway ->
            val utbetalingForUkjenteSaker: SisteUtbetalingerResponse = gateway.hentSisteUtbetalingISaker(
                SisteUtbetalingerRequest("007")
            )
            assertThat(utbetalingForUkjenteSaker.utbetalingsdato).isNull()
        }
    }

    @Test
    fun `Henter ut siste utbetaling by sakIdListe, kjent personId`() {
        withTestServer(h2) { gateway ->
            val utbetalingForKjenteSaker: SisteUtbetalingerResponse = gateway.hentSisteUtbetalingISaker(
                SisteUtbetalingerRequest(
                    "1"
                )
            )
            assertThat(utbetalingForKjenteSaker.utbetalingsdato)
                .isEqualTo(LocalDate.of(2023, 9, 12))
        }
    }

}
