package no.nav.aap.arenaoppslag

import no.nav.aap.arenaoppslag.client.ArenaOppslagGateway.Companion.withTestServer
import no.nav.aap.arenaoppslag.database.H2TestBase
import no.nav.aap.arenaoppslag.kontrakt.apiv1.MaksdatoSamordningRequest
import no.nav.aap.arenaoppslag.kontrakt.apiv1.MaksdatoSamordningResponse
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SamordningSisteVedtak
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SamordningVedtakStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SamordningApiTest : H2TestBase("flyway/saklistetest") {

    @Test
    fun `Henter ut maksdato by fodselsnummer, ukjent person`() {
        withTestServer(h2) { gateway ->
            val result = runCatching {
                gateway.hentMaksdatoSamordningByPerson(
                    MaksdatoSamordningRequest("ukjent")
                )
            }
            assertThat(result.isSuccess)
            assertThat(result.getOrThrow().sisteAktuelleVedtak).isNull()
        }
    }

    @Test
    fun `Henter ut maksdato by fodselsnummer, person uten AAP-vedtak ikke i Stans`() {
        withTestServer(h2) { gateway ->
            val maksdatoForUkjenteSaker: MaksdatoSamordningResponse = gateway.hentMaksdatoSamordningByPerson(
                MaksdatoSamordningRequest("annen ukjent")
            )
            assertThat(maksdatoForUkjenteSaker.sisteAktuelleVedtak).isNull()
        }
    }

    @Test
    fun `Henter ut maksdato by fodselsnummer, person med Stans-vedtak`() {
        withTestServer(h2) { gateway ->
            val maksdatoForUkjenteSaker: MaksdatoSamordningResponse = gateway.hentMaksdatoSamordningByPerson(
                MaksdatoSamordningRequest("maksdato100")
            )
            assertThat(maksdatoForUkjenteSaker.sisteAktuelleVedtak).isNotNull()
            val expected = SamordningSisteVedtak(
                vedtakId = 1109,
                saknummer = "2022-1102",
                maksdato = null,
                status = SamordningVedtakStatus.STANSET,
                harForlengelseEtter11_12 = false
            )
            assertThat(maksdatoForUkjenteSaker.sisteAktuelleVedtak).isEqualTo(expected)
        }
    }

    @Test
    fun `Henter ut maksdato by fodselsnummer, kjente saker`() {
        withTestServer(h2) { gateway ->
            // FakePdlGateway ekkoer fnr-en, slik at PersonService kan slå opp uten å gå mot PDL.
            val maksdatoForKjenteSaker: MaksdatoSamordningResponse = gateway.hentMaksdatoSamordningByPerson(
                MaksdatoSamordningRequest("maksdato102")
            )

            assertThat(maksdatoForKjenteSaker.sisteAktuelleVedtak?.vedtakId).isEqualTo(1122)
        }
    }

}
