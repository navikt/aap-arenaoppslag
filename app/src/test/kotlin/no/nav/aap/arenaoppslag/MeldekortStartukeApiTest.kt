package no.nav.aap.arenaoppslag

import io.ktor.client.plugins.*
import io.ktor.http.*
import no.nav.aap.arenaoppslag.client.ArenaOppslagGateway.Companion.withTestServer
import no.nav.aap.arenaoppslag.database.H2TestBase
import no.nav.aap.arenaoppslag.modeller.MeldekortStartukeRequest
import no.nav.aap.arenaoppslag.modeller.MeldekortStartukeResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MeldekortStartukeApiTest : H2TestBase("flyway/meldekort") {

    @Test
    fun `henter år og ukenummer for siste meldekort`() {
        withTestServer(h2) { gateway ->
            val response = gateway.hentMeldekortStartuke(MeldekortStartukeRequest("50000000001"))

            assertThat(response).isEqualTo(MeldekortStartukeResponse(aar = 2026, ukenummer = "53"))
        }
    }

    @Test
    fun `gir tomme felter når personen ikke har AAP-meldekort`() {
        withTestServer(h2) { gateway ->
            val response = gateway.hentMeldekortStartuke(MeldekortStartukeRequest("50000000003"))

            assertThat(response).isEqualTo(MeldekortStartukeResponse(aar = null, ukenummer = null))
        }
    }

    @Test
    fun `gir 404 for ukjent person`() {
        withTestServer(h2) { gateway ->
            val result = runCatching {
                gateway.hentMeldekortStartuke(MeldekortStartukeRequest("007"))
            }
            val error = result.exceptionOrNull() as? ClientRequestException
            assertThat(error).isNotNull
            assertThat(error!!.response.status).isEqualTo(HttpStatusCode.NotFound)
        }
    }
}

