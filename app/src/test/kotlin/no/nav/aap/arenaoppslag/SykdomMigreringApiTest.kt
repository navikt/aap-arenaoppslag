package no.nav.aap.arenaoppslag

import io.ktor.http.HttpStatusCode
import no.nav.aap.arenaoppslag.client.ArenaOppslagGateway.Companion.withTestServer
import no.nav.aap.arenaoppslag.database.H2TestBase
import no.nav.aap.arenaoppslag.kontrakt.migrering.ArenaDiagnose
import no.nav.aap.arenaoppslag.kontrakt.migrering.ArenaVilkar
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SykdomMigreringApiTest : H2TestBase("flyway/migrering") {

    @Test
    fun `henter begrunnelse og vilkår fra gjeldende 11-5-vedtak`() {
        withTestServer(h2) { gateway ->
            val respons = gateway.hentSykdomsvurdering("2023-504")

            assertThat(respons.vedtakId).isEqualTo(91045)
            assertThat(respons.begrunnelse).isEqualTo("Arbeidsevnen er nedsatt med minst halvparten")
            assertThat(respons.vilkar).containsExactlyInAnyOrder(
                ArenaVilkar(
                    id = 910451,
                    kode = "SYKSKADLYT",
                    status = "J",
                    begrunnelse = "Dokumentert gjennom legeerklæring",
                ),
                ArenaVilkar(
                    id = 910452,
                    kode = "INNTNEDS",
                    status = "N",
                    begrunnelse = null,
                ),
                ArenaVilkar(
                    id = 910453,
                    kode = "AAARBEVNE",
                    status = "V",
                    begrunnelse = null,
                ),
            )
            assertThat(respons.diagnoser).containsExactly(
                ArenaDiagnose("ICPC2", "L84", "HOVED", LocalDate.of(2023, 2, 1)),
                ArenaDiagnose("ICD10", "M54", "BI", LocalDate.of(2023, 3, 1)),
                ArenaDiagnose("ICPC2", "P76", "BI", LocalDate.of(2023, 4, 1)),
            )
        }
    }

    @Test
    fun `svarer 404 når saken ikke har gjeldende 11-5-vedtak`() {
        withTestServer(h2) { gateway ->
            assertThat(gateway.hentSykdomsvurderingStatus("2023-502")).isEqualTo(HttpStatusCode.NotFound)
        }
    }

    @Test
    fun `svarer 404 for ukjent saksnummer`() {
        withTestServer(h2) { gateway ->
            assertThat(gateway.hentSykdomsvurderingStatus("2099-99999")).isEqualTo(HttpStatusCode.NotFound)
        }
    }

    @Test
    fun `svarer 400 for ugyldig saksnummer`() {
        withTestServer(h2) { gateway ->
            assertThat(gateway.hentSykdomsvurderingStatus("ugyldig")).isEqualTo(HttpStatusCode.BadRequest)
        }
    }
}
