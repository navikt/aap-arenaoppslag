package no.nav.aap.arenaoppslag

import io.ktor.http.HttpStatusCode
import no.nav.aap.arenaoppslag.client.ArenaOppslagGateway.Companion.withTestServer
import no.nav.aap.arenaoppslag.database.H2TestBase
import no.nav.aap.arenaoppslag.kontrakt.migrering.ArenaForutgaaendeMedlemskapResponse
import no.nav.aap.arenaoppslag.kontrakt.migrering.ArenaVilkar
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ForutgaaendeMedlemskapMigreringApiTest : H2TestBase("flyway/migrering") {

    @Test
    fun `henter MEDLSKAP5-vilkåret fra gjeldende aap-vedtak`() {
        withTestServer(h2) { gateway ->
            assertThat(gateway.hentForutgaaendeMedlemskap("2023-505")).isEqualTo(
                ArenaForutgaaendeMedlemskapResponse(
                    vedtakId = 91051,
                    vilkar = ArenaVilkar(
                        id = 910513,
                        kode = "MEDLSKAP5",
                        status = "J",
                        begrunnelse = "Medlem i folketrygden siste fem år",
                    ),
                )
            )
        }
    }

    @Test
    fun `svarer 200 med null når vedtaket ikke har MEDLSKAP5`() {
        withTestServer(h2) { gateway ->
            assertThat(gateway.hentForutgaaendeMedlemskapStatus("2023-506")).isEqualTo(HttpStatusCode.OK)
            val respons = gateway.hentForutgaaendeMedlemskap("2023-506")
            assertThat(respons.vedtakId).isEqualTo(91061)
            assertThat(respons.vilkar).isNull()
        }
    }

    @Test
    fun `svarer 404 når saken ikke har gjeldende aap-vedtak`() {
        withTestServer(h2) { gateway ->
            assertThat(gateway.hentForutgaaendeMedlemskapStatus("2023-502")).isEqualTo(HttpStatusCode.NotFound)
        }
    }

    @Test
    fun `svarer 404 for ukjent saksnummer`() {
        withTestServer(h2) { gateway ->
            assertThat(gateway.hentForutgaaendeMedlemskapStatus("2099-99999")).isEqualTo(HttpStatusCode.NotFound)
        }
    }
}
