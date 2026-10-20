package no.nav.aap.arenaoppslag

import io.ktor.http.HttpStatusCode
import no.nav.aap.arenaoppslag.client.ArenaOppslagGateway.Companion.withTestServer
import no.nav.aap.arenaoppslag.database.H2TestBase
import no.nav.aap.arenaoppslag.kontrakt.migrering.ArenaVedtaksperiodeResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VedtaksperiodeMigreringApiTest : H2TestBase("flyway/migrering") {

    @Test
    fun `henter fra- og tildato fra gjeldende aap-vedtak`() {
        withTestServer(h2) { gateway ->
            // Testdataen har et løpende vedtak som startet for 30 dager siden, uten tildato
            assertThat(gateway.hentVedtaksperiode("2023-505")).isEqualTo(
                ArenaVedtaksperiodeResponse(
                    vedtakId = 91051,
                    fraDato = LocalDate.now().minusDays(30),
                    tilDato = null,
                )
            )
        }
    }

    @Test
    fun `svarer 404 når saken ikke har gjeldende aap-vedtak`() {
        withTestServer(h2) { gateway ->
            assertThat(gateway.hentVedtaksperiodeStatus("2023-502")).isEqualTo(HttpStatusCode.NotFound)
        }
    }

    @Test
    fun `svarer 404 for ukjent saksnummer`() {
        withTestServer(h2) { gateway ->
            assertThat(gateway.hentVedtaksperiodeStatus("2099-99999")).isEqualTo(HttpStatusCode.NotFound)
        }
    }
}
