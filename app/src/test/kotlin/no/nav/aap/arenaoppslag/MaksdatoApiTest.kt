package no.nav.aap.arenaoppslag

import io.ktor.client.plugins.*
import io.ktor.http.*
import no.nav.aap.arenaoppslag.client.ArenaOppslagGateway.Companion.withTestServer
import no.nav.aap.arenaoppslag.database.H2TestBase
import no.nav.aap.arenaoppslag.kontrakt.apiv1.MaksdatoMedVedtakResponse
import no.nav.aap.arenaoppslag.kontrakt.apiv1.MaksdatoRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MaksdatoApiTest : H2TestBase("flyway/saklistetest") {

    @Test
    fun `Henter ut maksdato by fodselsnummer, ukjent person`() {
        withTestServer(h2) { gateway ->
            val result = runCatching {
                gateway.hentMaksdatoByPerson(
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
            val maksdatoForUkjenteSaker: MaksdatoMedVedtakResponse = gateway.hentMaksdatoByPerson(
                MaksdatoRequest("annen101")
            )
            assertThat(maksdatoForUkjenteSaker.sak).isNull()
        }
    }

    @Test
    fun `Henter ut maksdato by fodselsnummer, kjente saker`() {
        withTestServer(h2) { gateway ->
            // FakePdlGateway ekkoer fnr-en, slik at PersonService kan slå opp uten å gå mot PDL.
            val maksdatoForKjenteSaker: MaksdatoMedVedtakResponse = gateway.hentMaksdatoByPerson(
                MaksdatoRequest("maksdato102")
            )

            assertThat(maksdatoForKjenteSaker.sak?.sisteVedtak?.vedtakId).isEqualTo(1122)
        }
    }

}
