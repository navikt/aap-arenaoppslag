package no.nav.aap.arenaoppslag

import io.ktor.http.HttpStatusCode
import no.nav.aap.arenaoppslag.client.ArenaOppslagGateway.Companion.withTestServer
import no.nav.aap.arenaoppslag.database.H2TestBase
import no.nav.aap.arenaoppslag.kontrakt.migrering.ArenaRefusjonskrav
import no.nav.aap.arenaoppslag.kontrakt.migrering.ArenaRefusjonskravResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class RefusjonskravMigreringApiTest : H2TestBase("flyway/migrering") {

    @Test
    fun `henter refusjonskrav fra vedtaksfakta på gjeldende 11-5-vedtak`() {
        withTestServer(h2) { gateway ->
            val respons = gateway.hentRefusjonskrav("2023-505")

            assertThat(respons).isEqualTo(
                ArenaRefusjonskravResponse(
                    ArenaRefusjonskrav(
                        aarsak = "REFKRAVSOS",
                        fraDato = LocalDate.of(2024, 2, 1),
                        tilDato = LocalDate.of(2024, 3, 31),
                    )
                )
            )
        }
    }

    @Test
    fun `svarer 200 med null når gjeldende 11-5-vedtak ikke har refusjonskrav`() {
        withTestServer(h2) { gateway ->
            assertThat(gateway.hentRefusjonskravStatus("2023-506")).isEqualTo(HttpStatusCode.OK)
            assertThat(gateway.hentRefusjonskrav("2023-506").refusjonskrav).isNull()
        }
    }

    @Test
    fun `svarer 404 når saken ikke har gjeldende 11-5-vedtak`() {
        withTestServer(h2) { gateway ->
            assertThat(gateway.hentRefusjonskravStatus("2023-502")).isEqualTo(HttpStatusCode.NotFound)
        }
    }

    @Test
    fun `svarer 404 for ukjent saksnummer`() {
        withTestServer(h2) { gateway ->
            assertThat(gateway.hentRefusjonskravStatus("2099-99999")).isEqualTo(HttpStatusCode.NotFound)
        }
    }

    @Test
    fun `svarer 400 for ugyldig saksnummer`() {
        withTestServer(h2) { gateway ->
            assertThat(gateway.hentRefusjonskravStatus("ugyldig")).isEqualTo(HttpStatusCode.BadRequest)
        }
    }
}
