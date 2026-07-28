package no.nav.aap.arenaoppslag

import io.ktor.client.plugins.*
import io.ktor.http.*
import no.nav.aap.arenaoppslag.client.ArenaOppslagGateway.Companion.withTestServer
import no.nav.aap.arenaoppslag.database.H2TestBase
import no.nav.aap.arenaoppslag.kontrakt.apiv1.ManuellFordelingsgrunnlagRequest
import no.nav.aap.arenaoppslag.kontrakt.apiv1.ManuellFordelingsgrunnlagResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ManuellFordelingsgrunnlagApiTest : H2TestBase("flyway/saklistetest") {

    @Test
    fun `ukjent person gir 404`() {
        withTestServer(h2) { gateway ->
            val result = runCatching {
                gateway.hentManuellFordelingsgrunnlag(ManuellFordelingsgrunnlagRequest("ukjent"))
            }
            val error = result.exceptionOrNull() as? ClientRequestException
            assertThat(error).isNotNull
            assertThat(error!!.response.status).isEqualTo(HttpStatusCode.NotFound)
        }
    }

    @Test
    fun `kjent person med AAP-sak returnerer grunnlag med siste vedtak`() {
        withTestServer(h2) { gateway ->
            val grunnlag: ManuellFordelingsgrunnlagResponse =
                gateway.hentManuellFordelingsgrunnlag(ManuellFordelingsgrunnlagRequest("maksdato102"))

            assertThat(grunnlag.sisteVedtak?.vedtakId).isEqualTo(1122)
        }
    }
}

