package no.nav.aap.arenaoppslag

import io.ktor.server.testing.*
import no.nav.aap.arenaoppslag.TestConfig.jsonHttpClient
import no.nav.aap.arenaoppslag.client.ArenaOppslagGateway
import no.nav.aap.arenaoppslag.database.H2TestBase
import no.nav.aap.arenaoppslag.kontrakt.intern.NyereSakerRequest
import no.nav.aap.arenaoppslag.kontrakt.intern.NyereSakerResponse
import no.nav.aap.arenaoppslag.kontrakt.intern.SignifikanteSakerRequest
import no.nav.aap.arenaoppslag.kontrakt.intern.SignifikanteSakerResponse
import no.nav.aap.arenaoppslag.util.AzureTokenGen
import no.nav.aap.arenaoppslag.util.Fakes
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class HistorikkApiTest : H2TestBase("flyway/minimumtest", "flyway/eksisterer") {
    companion object {
        const val ukjentPerson = "007"
        const val kjentPerson = "kun_nye"
    }

    @Test
    fun `Person har signifikant historikk i AAP-Arena`() {
        withTestServer { gateway ->
            val kjentPerson: SignifikanteSakerResponse = gateway.personHarSignifikantAAPArenaHistorikk(
                SignifikanteSakerRequest(
                    personidentifikatorer = listOf(kjentPerson),
                    virkningstidspunkt = LocalDateTime.now().minusDays(1).toLocalDate()
                )
            )

            assertThat(kjentPerson.harSignifikantHistorikk).isTrue
        }
    }

    @Test
    fun `Person har IKKE signifikant historikk i AAP-Arena`() {
        withTestServer { gateway ->
            val ukjentPerson: SignifikanteSakerResponse = gateway.personHarSignifikantAAPArenaHistorikk(
                SignifikanteSakerRequest(
                    personidentifikatorer = listOf(ukjentPerson),
                    virkningstidspunkt = LocalDateTime.now().minusDays(1).toLocalDate()
                )
            )

            assertThat(ukjentPerson.harSignifikantHistorikk).isFalse
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
