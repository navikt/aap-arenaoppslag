package no.nav.aap.arenaoppslag

import io.ktor.http.HttpStatusCode
import no.nav.aap.arenaoppslag.client.ArenaOppslagGateway.Companion.withTestServer
import no.nav.aap.arenaoppslag.database.H2TestBase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

// Kvotehistorikk eksponeres av /api/intern/sak/{sakid}/kvotehistorikk.
class KvotehistorikkApiTest : H2TestBase("flyway/maksimum") {

    @Test
    fun `responsen inneholder kvotebevegelser for personen bak saken`() {
        withTestServer(h2) { gateway ->
            val kvotehistorikk = gateway.hentKvotehistorikk("2023-9001")

            assertThat(kvotehistorikk).hasSize(5)

            val init = kvotehistorikk.first { it.id == 200 }
            assertThat(init.kvoteTypeKode).isEqualTo("AAP")
            assertThat(init.posteringTypeKode).isEqualTo("INIT")
            assertThat(init.antallBevegelse).isEqualTo(20)
            assertThat(init.datoHendelse).isEqualTo(LocalDate.of(2023, 1, 1))

            val meldekortbevegelse = kvotehistorikk.first { it.id == 202 }
            assertThat(meldekortbevegelse.kvoteTypeKode).isEqualTo("AAP")
            assertThat(meldekortbevegelse.endringsGrunnlag).isEqualTo("MKORT")
            assertThat(meldekortbevegelse.posteringTypeKode).isEqualTo("OPPD")
            assertThat(meldekortbevegelse.objektIdGrunnlag).isEqualTo(5001L)
            assertThat(meldekortbevegelse.antallBevegelse).isEqualTo(-10)
        }
    }

    @Test
    fun `ugyldig saksnummerformat gir 400`() {
        withTestServer(h2) { gateway ->
            assertThat(gateway.hentKvotehistorikkStatus("99999")).isEqualTo(HttpStatusCode.BadRequest)
        }
    }

    @Test
    fun `ukjent sak gir 404`() {
        withTestServer(h2) { gateway ->
            assertThat(gateway.hentKvotehistorikkStatus("2023-99999")).isEqualTo(HttpStatusCode.NotFound)
        }
    }
}
