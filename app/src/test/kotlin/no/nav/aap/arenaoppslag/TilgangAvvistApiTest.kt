package no.nav.aap.arenaoppslag

import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.aap.arenaoppslag.client.ArenaOppslagGateway.Companion.withTestServer
import no.nav.aap.arenaoppslag.database.H2TestBase
import no.nav.aap.arenaoppslag.kontrakt.apiv1.VedtakForPersonRequest
import no.nav.aap.arenaoppslag.tilgangsmaskin.HarTilgangFraTilgangsmaskinen
import no.nav.aap.arenaoppslag.tilgangsmaskin.TilgangmaskinGateway
import no.nav.aap.arenaoppslag.tilgangsmaskin.TilgangsmaskinAvvistResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TilgangAvvistApiTest : H2TestBase("flyway/minimumtest") {
    @Test
    fun `returnerer avslagsdetaljer ved avvist tilgang`() {
        verifiserAvvistResponse(
            TilgangsmaskinAvvistResponse(
                type = "AVVIST_GEOGRAFISK",
                title = "AVVIST_GEOGRAFISK",
                status = 403,
                begrunnelse = "Mangler geografisk tilgang",
                kanOverstyres = true,
            ),
            """{"title":"AVVIST_GEOGRAFISK","begrunnelse":"Mangler geografisk tilgang","kanOverstyres":true}""",
        )
    }

    @Test
    fun `returnerer 403 uten body naar avslagsdetaljer mangler`() {
        verifiserAvvistResponse(null, null)
    }

    @Test
    fun `returnerer at avslag ikke kan overstyres`() {
        verifiserAvvistResponse(
            TilgangsmaskinAvvistResponse(
                type = "AVVIST_GEOGRAFISK",
                title = "AVVIST_GEOGRAFISK",
                status = 403,
                begrunnelse = "Mangler geografisk tilgang",
                kanOverstyres = false,
            ),
            """{"title":"AVVIST_GEOGRAFISK","begrunnelse":"Mangler geografisk tilgang","kanOverstyres":false}""",
        )
    }

    private fun verifiserAvvistResponse(
        avvistResponse: TilgangsmaskinAvvistResponse?,
        forventetJson: String?,
    ) {
        val tilgangmaskinGateway = mockk<TilgangmaskinGateway>(relaxed = true)
        coEvery { tilgangmaskinGateway.harTilgangTilPerson(any(), any()) } returns
            HarTilgangFraTilgangsmaskinen(harTilgang = false, avvistResponse = avvistResponse)

        withTestServer(h2, tilgangmaskinGateway, navIdent = "Z123456") { gateway ->
            val kall: List<suspend () -> Unit> = listOf(
                { gateway.hentVedtakForPerson(VedtakForPersonRequest("123")) },
                { gateway.hentVedtakDetaljerForPerson(VedtakForPersonRequest("123")) },
                { gateway.hentVedtakForPerson(VedtakForPersonRequest("007")) },
                { gateway.hentVedtakDetaljerForPerson(VedtakForPersonRequest("007")) },
                { gateway.hentSak("2021-1") },
            )
            val mapper = DefaultJsonMapper.objectMapper()
            for (hent in kall) {
                val feil = assertThrows<ClientRequestException> { hent() }
                assertThat(feil.response.status).isEqualTo(HttpStatusCode.Forbidden)
                if (forventetJson == null) {
                    assertThat(feil.response.bodyAsText()).isEmpty()
                } else {
                    assertThat(feil.response.contentType()?.withoutParameters()).isEqualTo(ContentType.Application.Json)
                    assertThat(mapper.readTree(feil.response.bodyAsText())).isEqualTo(mapper.readTree(forventetJson))
                }
            }
        }
    }
}
