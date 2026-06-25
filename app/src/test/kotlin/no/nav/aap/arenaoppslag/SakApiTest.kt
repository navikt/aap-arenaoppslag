package no.nav.aap.arenaoppslag

import no.nav.aap.arenaoppslag.client.ArenaOppslagGateway.Companion.withTestServer
import no.nav.aap.arenaoppslag.database.H2TestBase
import no.nav.aap.arenaoppslag.kontrakt.apiv1.ArenaSakMedVedtakResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SakApiTest : H2TestBase("flyway/minimumtest") {

    @Test
    fun `Henter sak med vedtak for kjent sakId`() {
        withTestServer(h2) { gateway ->
            val sak: ArenaSakMedVedtakResponse = gateway.hentSak("1")

            assertThat(sak.sakId).isEqualTo("1")
            assertThat(sak.opprettetAar).isEqualTo(2021)
            assertThat(sak.lopenr).isEqualTo(1)
            assertThat(sak.statuskode).isEqualTo("AKTIV")
            assertThat(sak.person.fodselsnummer).isEqualTo("123")
            assertThat(sak.vedtak).hasSize(1)
        }
    }

    @Test
    fun `Henter sak med vedtak via saksnummer`() {
        withTestServer(h2) { gateway ->
            val sak: ArenaSakMedVedtakResponse = gateway.hentSak("2021-1")

            assertThat(sak.sakId).isEqualTo("1")
            assertThat(sak.vedtak).hasSize(1)
        }
    }

    @Test
    fun `Returnerer 404 for ukjent sakId`() {
        withTestServer(h2) { gateway ->
            val statusKode = runCatching { gateway.hentSak("99999") }
                .exceptionOrNull()
                ?.message

            assertThat(statusKode).contains("404")
        }
    }
}
