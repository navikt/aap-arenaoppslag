package no.nav.aap.arenaoppslag

import no.nav.aap.arenaoppslag.client.ArenaOppslagGateway.Companion.withTestServer
import no.nav.aap.arenaoppslag.database.H2TestBase
import no.nav.aap.arenaoppslag.kontrakt.intern.PersonEksistererIAAPArena
import no.nav.aap.arenaoppslag.kontrakt.intern.SakerRequest
import no.nav.aap.arenaoppslag.kontrakt.intern.SignifikanteSakerRequest
import no.nav.aap.arenaoppslag.kontrakt.intern.SignifikanteSakerResponse
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
        withTestServer(h2) { gateway ->
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
        withTestServer(h2) { gateway ->
            val ukjentPerson: SignifikanteSakerResponse = gateway.personHarSignifikantAAPArenaHistorikk(
                SignifikanteSakerRequest(
                    personidentifikatorer = listOf(ukjentPerson),
                    virkningstidspunkt = LocalDateTime.now().minusDays(1).toLocalDate()
                )
            )

            assertThat(ukjentPerson.harSignifikantHistorikk).isFalse
        }
    }

    @Test
    fun `Person eksisterer i AAP-Arena, ja`() {
        withTestServer(h2) { gateway ->
            val kjentPerson: PersonEksistererIAAPArena = gateway.hentPersonEksistererIAapContext(
                SakerRequest(
                    personidentifikatorer = listOf(kjentPerson)
                )
            )
            assertThat(kjentPerson.eksisterer).isTrue
        }
    }

    @Test
    fun `Person eksisterer i AAP-Arena, nei`() {
        withTestServer(h2) { gateway ->
            val ukjentPerson: PersonEksistererIAAPArena = gateway.hentPersonEksistererIAapContext(
                SakerRequest(
                    personidentifikatorer = listOf(ukjentPerson)
                )
            )
            assertThat(ukjentPerson.eksisterer).isFalse
        }
    }

}
