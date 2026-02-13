package no.nav.aap.arenaoppslag

import io.ktor.server.testing.*
import no.nav.aap.arenaoppslag.TestConfig.jsonHttpClient
import no.nav.aap.arenaoppslag.client.ArenaOppslagGateway
import no.nav.aap.arenaoppslag.database.H2TestBase
import no.nav.aap.arenaoppslag.kontrakt.intern.NyereSakerRequest
import no.nav.aap.arenaoppslag.kontrakt.intern.NyereSakerResponse
import no.nav.aap.arenaoppslag.util.AzureTokenGen
import no.nav.aap.arenaoppslag.util.Fakes
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class HistorikkApiTest : H2TestBase("flyway/minimumtest", "flyway/eksisterer") {
    companion object {
        const val ukjentPerson = "007"
        const val kjentPerson = "1"
    }

    @Test
    fun `Person har nyere historikk i AAP-Arena`() {
        withTestServer { gateway ->
            val kjentPerson: NyereSakerResponse = gateway.personHarNyereAapArenaHistorikk(
                NyereSakerRequest(
                    personidentifikatorer = listOf(kjentPerson),
                )
            )

            assertThat(kjentPerson.eksisterer).isTrue
        }
    }

    @Test
    fun `Person har IKKE nyere historikk i AAP-Arena`() {
        withTestServer { gateway ->
            val kjentPerson: NyereSakerResponse = gateway.personHarNyereAapArenaHistorikk(
                NyereSakerRequest(
                    personidentifikatorer = listOf(ukjentPerson),
                )
            )

            assertThat(kjentPerson.eksisterer).isFalse
        }
    }


    private fun withTestServer(testBody: suspend (ArenaOppslagGateway) -> Unit) {
        val config = TestConfig.default(Fakes())
        val tokenProvider = AzureTokenGen(config.azure.issuer, config.azure.clientId)
        testApplication {
            application { server(config, h2) }
            val gateway = ArenaOppslagGateway(tokenProvider, jsonHttpClient)

            testBody(gateway)
        }
    }

}
