package no.nav.aap.arenaoppslag

import io.ktor.http.HttpStatusCode
import no.nav.aap.arenaoppslag.client.ArenaOppslagGateway.Companion.withTestServer
import no.nav.aap.arenaoppslag.database.H2TestBase
import no.nav.aap.arenaoppslag.kontrakt.migrering.ArenaBeregningstidspunktResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BeregningstidspunktMigreringApiTest : H2TestBase("flyway/migrering") {

    @Test
    fun `henter beregningstidspunkt fra AAPBERDATO på siste løpende aap-vedtak`() {
        withTestServer(h2) { gateway ->
            assertThat(gateway.hentBeregningstidspunkt("2023-507")).isEqualTo(
                ArenaBeregningstidspunktResponse(LocalDate.of(2024, 3, 15))
            )
        }
    }

    @Test
    fun `svarer 200 med null når vedtaket ikke har AAPBERDATO`() {
        withTestServer(h2) { gateway ->
            assertThat(gateway.hentBeregningstidspunktStatus("2023-508")).isEqualTo(HttpStatusCode.OK)
            assertThat(gateway.hentBeregningstidspunkt("2023-508").beregningstidspunkt).isNull()
        }
    }

    @Test
    fun `svarer 404 når saken ikke har løpende aap-vedtak`() {
        withTestServer(h2) { gateway ->
            assertThat(gateway.hentBeregningstidspunktStatus("2023-502")).isEqualTo(HttpStatusCode.NotFound)
        }
    }

    @Test
    fun `svarer 404 for ukjent saksnummer`() {
        withTestServer(h2) { gateway ->
            assertThat(gateway.hentBeregningstidspunktStatus("2099-99999")).isEqualTo(HttpStatusCode.NotFound)
        }
    }

    @Test
    fun `svarer 400 for ugyldig saksnummer`() {
        withTestServer(h2) { gateway ->
            assertThat(gateway.hentBeregningstidspunktStatus("ugyldig")).isEqualTo(HttpStatusCode.BadRequest)
        }
    }
}
