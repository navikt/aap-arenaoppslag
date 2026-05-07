package no.nav.aap.arenaoppslag

import no.nav.aap.arenaoppslag.client.ArenaOppslagGateway.Companion.withTestServer
import no.nav.aap.arenaoppslag.database.H2TestBase
import no.nav.aap.arenaoppslag.kontrakt.apiv1.MaksdatoRequest
import no.nav.aap.arenaoppslag.kontrakt.apiv1.MaksdatoResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MaksdatoApiTest : H2TestBase("flyway/minimumtest", "flyway/eksisterer") {

    @Test
    fun `Henter ut maksdato by sakIdListe, ukjente saker`() {
        withTestServer(h2) { gateway ->
            val maksdatoForUkjenteSaker: MaksdatoResponse = gateway.hentMaksdatoBySakIdListe(
                MaksdatoRequest(
                    saker = listOf(
                        /* ukjente verdier */ 1001, 1002,
                        /* kjente verdier */ 1234
                    )
                )
            )
            assertThat(maksdatoForUkjenteSaker.sakliste).isEmpty()
        }
    }

    @Test
    fun `Henter ut maksdato by sakIdListe, kjente saker`() {
        withTestServer(h2) { gateway ->
            val maksdatoForKjenteSaker: MaksdatoResponse = gateway.hentMaksdatoBySakIdListe(
                MaksdatoRequest(
                    saker = listOf(1, 2, 3)
                )
            )

            assertThat(maksdatoForKjenteSaker.sakliste.map { it.sisteVedtak.vedtakId }).isEqualTo(listOf(1335, 30))
        }
    }

}
