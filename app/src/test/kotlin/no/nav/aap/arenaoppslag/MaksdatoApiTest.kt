package no.nav.aap.arenaoppslag

import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import no.nav.aap.arenaoppslag.client.ArenaOppslagGateway.Companion.withTestServer
import no.nav.aap.arenaoppslag.database.H2TestBase
import no.nav.aap.arenaoppslag.kontrakt.apiv1.MaksdatoRequest
import no.nav.aap.arenaoppslag.kontrakt.apiv1.MaksdatoResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MaksdatoApiTest : H2TestBase("flyway/saklistetest") {

    @Test
    fun `Henter ut maksdato by fodselsnummer, ukjent person`() {
        withTestServer(h2) { gateway ->
            val result = runCatching {
                gateway.hentMaksdatoBySakIdListe(
                    MaksdatoRequest("ukjent")
                )
            }
            val error = result.exceptionOrNull() as? ClientRequestException
            assertThat(error).isNotNull
            assertThat(error!!.response.status).isEqualTo(HttpStatusCode.NotFound)
        }
    }

    @Test
    fun `Henter ut maksdato by fodselsnummer, person uten AAP-vedtak ikke i Stans`() {
        withTestServer(h2) { gateway ->
            val maksdatoForUkjenteSaker: MaksdatoResponse = gateway.hentMaksdatoBySakIdListe(
                MaksdatoRequest("annen101")
            )
            assertThat(maksdatoForUkjenteSaker.sakliste).isEmpty()
        }
    }

    @Test
    fun `Henter ut maksdato by fodselsnummer, kjente saker`() {
        withTestServer(h2) { gateway ->
            // FakePdlGateway ekkoer fnr-en, slik at PersonService kan slå opp uten å gå mot PDL.
            val maksdatoForKjenteSaker: MaksdatoResponse = gateway.hentMaksdatoBySakIdListe(
                MaksdatoRequest("maksdato100")
            )

            assertThat(maksdatoForKjenteSaker.sakliste.map { it.sisteVedtak.vedtakId })
                .containsExactly(1103, 1101)
        }
    }

}
