package no.nav.aap.arenaoppslag

import no.nav.aap.arenaoppslag.client.ArenaOppslagGateway.Companion.withTestServer
import no.nav.aap.arenaoppslag.database.H2TestBase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

// Vedtakfakta eksponeres av /api/intern/vedtak/{vedtakId}/fakta.
class VedtakfaktaApiTest : H2TestBase("flyway/vedtakfakta") {

    @Test
    fun `Henter alle vedtakfakta for kjent vedtak`() {
        withTestServer(h2) { gateway ->
            val respons = gateway.hentVedtakfakta("1234")

            assertThat(respons.fakta.map { it.kode })
                .containsExactlyInAnyOrder("UNNTAKAAP", "AAPVILKUNN")
            assertThat(respons.fakta.first { it.kode == "UNNTAKAAP" }.verdi).isEqualTo("J")
            assertThat(respons.fakta.first { it.kode == "AAPVILKUNN" }.registrertDato)
                .isEqualTo(LocalDate.of(2025, 3, 28))
        }
    }

    @Test
    fun `Ukjent vedtak gir tom liste`() {
        withTestServer(h2) { gateway ->
            val respons = gateway.hentVedtakfakta("999999999")

            assertThat(respons.fakta).isEmpty()
        }
    }

    @Test
    fun `ugyldig vedtakIdformat gir 400`() {
        withTestServer(h2) { gateway ->
            val feilmelding = runCatching { gateway.hentVedtakfakta("ikke-et-tall") }
                .exceptionOrNull()
                ?.message

            assertThat(feilmelding).contains("400")
        }
    }
}



