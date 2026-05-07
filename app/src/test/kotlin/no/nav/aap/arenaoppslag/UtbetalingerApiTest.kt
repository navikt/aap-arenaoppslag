package no.nav.aap.arenaoppslag

import no.nav.aap.arenaoppslag.client.ArenaOppslagGateway.Companion.withTestServer
import no.nav.aap.arenaoppslag.database.H2TestBase
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SakMedSisteUtbetaling
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SisteUtbetalingerRequest
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SisteUtbetalingerResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class UtbetalingerApiTest : H2TestBase("flyway/postering") {

    @Test
    fun `Henter ut siste utbetaling by sakIdListe, ukjente saker`() {
        withTestServer(h2) { gateway ->
            val utbetalingForUkjenteSaker: SisteUtbetalingerResponse = gateway.hentSisteUtbetalingISaker(
                SisteUtbetalingerRequest(
                    saker = listOf(
                        /* ukjente verdier */ 1001, 1002,
                    )
                )
            )
            assertThat(utbetalingForUkjenteSaker.sakliste).containsExactly(
                SakMedSisteUtbetaling(sakId = 1001, sisteAAPUtbetalingsdato = null),
                SakMedSisteUtbetaling(sakId = 1002, sisteAAPUtbetalingsdato = null)
            )
        }
    }

    @Test
    fun `Henter ut siste utbetaling by sakIdListe, kjente saker`() {
        withTestServer(h2) { gateway ->
            val utbetalingForKjenteSaker: SisteUtbetalingerResponse = gateway.hentSisteUtbetalingISaker(
                SisteUtbetalingerRequest(
                    saker = listOf(1, 2, 3)
                )
            )
            assertThat(utbetalingForKjenteSaker.sakliste).containsExactly(
                SakMedSisteUtbetaling(1, LocalDate.of(2023, 9, 12)),
                SakMedSisteUtbetaling(2, null),
                SakMedSisteUtbetaling(3, null),
            )
        }
    }

}
