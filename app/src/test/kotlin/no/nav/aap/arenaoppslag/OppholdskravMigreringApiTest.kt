package no.nav.aap.arenaoppslag

import io.ktor.http.HttpStatusCode
import no.nav.aap.arenaoppslag.client.ArenaOppslagGateway.Companion.withTestServer
import no.nav.aap.arenaoppslag.database.H2TestBase
import no.nav.aap.arenaoppslag.kontrakt.migrering.ArenaOppholdskravResponse
import no.nav.aap.arenaoppslag.kontrakt.migrering.ArenaVilkar
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OppholdskravMigreringApiTest : H2TestBase("flyway/migrering") {

    @Test
    fun `henter AOINORGE3-vilkåret fra gjeldende aap-vedtak`() {
        withTestServer(h2) { gateway ->
            assertThat(gateway.hentOppholdskrav("2023-505")).isEqualTo(
                ArenaOppholdskravResponse(
                    vedtakId = 91051,
                    vilkar = ArenaVilkar(id = 910511, kode = "AOINORGE3", status = "J", begrunnelse = null),
                )
            )
        }
    }

    @Test
    fun `svarer 200 med null når vedtaket ikke har AOINORGE3`() {
        withTestServer(h2) { gateway ->
            assertThat(gateway.hentOppholdskravStatus("2023-506")).isEqualTo(HttpStatusCode.OK)
            val respons = gateway.hentOppholdskrav("2023-506")
            assertThat(respons.vedtakId).isEqualTo(91061)
            assertThat(respons.vilkar).isNull()
        }
    }

    @Test
    fun `svarer 404 når saken ikke har gjeldende aap-vedtak`() {
        withTestServer(h2) { gateway ->
            assertThat(gateway.hentOppholdskravStatus("2023-502")).isEqualTo(HttpStatusCode.NotFound)
        }
    }

    @Test
    fun `svarer 404 for ukjent saksnummer`() {
        withTestServer(h2) { gateway ->
            assertThat(gateway.hentOppholdskravStatus("2099-99999")).isEqualTo(HttpStatusCode.NotFound)
        }
    }
}
