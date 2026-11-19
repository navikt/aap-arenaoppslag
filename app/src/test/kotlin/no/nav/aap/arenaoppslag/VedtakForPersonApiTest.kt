package no.nav.aap.arenaoppslag

import io.ktor.client.plugins.*
import io.ktor.http.*
import no.nav.aap.arenaoppslag.client.ArenaOppslagGateway.Companion.withTestServer
import no.nav.aap.arenaoppslag.database.H2TestBase
import no.nav.aap.arenaoppslag.kontrakt.apiv1.ArenaVedtak
import no.nav.aap.arenaoppslag.kontrakt.apiv1.ArenaVedtakMedDetaljer
import no.nav.aap.arenaoppslag.kontrakt.apiv1.VedtakForPersonRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class VedtakForPersonApiTest : H2TestBase("flyway/minimumtest") {

    @Test
    fun `Henter vedtak (udetaljert) for kjent person`() {
        withTestServer(h2) { gateway ->
            val vedtak: List<ArenaVedtak> =
                gateway.hentVedtakForPerson(VedtakForPersonRequest("00000000123"))

            assertThat(vedtak).isNotEmpty()
            assertThat(vedtak.first().rettighetkode).isEqualTo("AAP")
        }
    }

    @Test
    fun `Returnerer 404 for ukjent person (udetaljert)`() {
        withTestServer(h2) { gateway ->
            val resultat = runCatching {
                gateway.hentVedtakForPerson(VedtakForPersonRequest("00000000007"))
            }
            val feil = resultat.exceptionOrNull() as? ClientRequestException
            assertThat(feil).isNotNull
            assertThat(feil!!.response.status).isEqualTo(HttpStatusCode.NotFound)
        }
    }

    @Test
    fun `Henter tom liste for person uten saker (udetaljert)`() {
        withTestServer(h2) { gateway ->
            val vedtak: List<ArenaVedtak> =
                gateway.hentVedtakForPerson(VedtakForPersonRequest("00000000003"))

            assertThat(vedtak).isEmpty()
        }
    }

    @Test
    fun `Henter vedtak for kjent person`() {
        withTestServer(h2) { gateway ->
            // Person med fodselsnr '00000000123' har én sak med ett vedtak (id=1234)
            val vedtak: List<ArenaVedtakMedDetaljer> =
                gateway.hentVedtakDetaljerForPerson(VedtakForPersonRequest("00000000123"))

            assertThat(vedtak).isNotEmpty()
            assertThat(vedtak.first().vedtakId).isEqualTo(1234)
        }
    }

    @Test
    fun `Returnerer 404 for ukjent person`() {
        withTestServer(h2) { gateway ->
            val resultat = runCatching {
                gateway.hentVedtakDetaljerForPerson(VedtakForPersonRequest("00000000007"))
            }
            val feil = resultat.exceptionOrNull() as? ClientRequestException
            assertThat(feil).isNotNull
            assertThat(feil!!.response.status).isEqualTo(HttpStatusCode.NotFound)
        }
    }

    @Test
    fun `Henter tom liste for person uten saker`() {
        withTestServer(h2) { gateway ->
            // Person med fodselsnr '00000000003' eksisterer i Arena men har ingen saker
            val vedtak: List<ArenaVedtakMedDetaljer> =
                gateway.hentVedtakDetaljerForPerson(VedtakForPersonRequest("00000000003"))

            assertThat(vedtak).isEmpty()
        }
    }
}
