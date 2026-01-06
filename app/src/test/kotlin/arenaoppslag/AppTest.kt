package arenaoppslag

import arenaoppslag.util.AzureTokenGen
import arenaoppslag.util.Fakes
import arenaoppslag.util.H2TestBase
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.request
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.testing.*
import no.nav.aap.arenaoppslag.kontrakt.ekstern.EksternVedtakRequest
import no.nav.aap.arenaoppslag.kontrakt.ekstern.VedtakResponse
import no.nav.aap.arenaoppslag.kontrakt.modeller.Maksimum
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class AppTest : H2TestBase() {

    @Test
    fun `Henter ut vedtak for fellesordningen`() {
        Fakes().use { fakes ->
            val config = TestConfig.default(fakes)
            val azure = AzureTokenGen(config.azure.issuer, config.azure.clientId)

            testApplication {
                application { server(config, h2) }

                val res = jsonHttpClient.post("/intern/perioder") {
                    bearerAuth(azure.generate())
                    contentType(ContentType.Application.Json)
                    setBody(
                        EksternVedtakRequest(
                            personidentifikator = "1",
                            fraOgMedDato = LocalDate.of(2022, 10, 1),
                            tilOgMedDato = LocalDate.of(2023, 12, 31)
                        )
                    )
                }

                assertEquals(HttpStatusCode.OK, res.status)

                println(res.body<String>())
                val alleVedtak = res.body<VedtakResponse>()

                assertEquals(1, alleVedtak.perioder.size)
            }
        }
    }

    @Test
    fun `Henter ut maksimumsvedtak for fellesordningen`() {
        Fakes().use { fakes ->
            val config = TestConfig.default(fakes)
            val azure = AzureTokenGen(config.azure.issuer, config.azure.clientId)

            testApplication {
                application { server(config, h2) }

                val res = jsonHttpClient.post("/intern/maksimum") {
                    bearerAuth(azure.generate())
                    contentType(ContentType.Application.Json)
                    setBody(
                        EksternVedtakRequest(
                            personidentifikator = "1",
                            fraOgMedDato = LocalDate.of(2022, 10, 1),
                            tilOgMedDato = LocalDate.of(2023, 12, 31)
                        )
                    )
                }

                assertEquals(HttpStatusCode.OK, res.status)

                val alleVedtak = res.body<Maksimum>()

                assertEquals(1, alleVedtak.vedtak.size)
            }
        }
    }


    @Test
    fun `Henter ut saker by fnr`() {
        Fakes().use { fakes ->
            val config = TestConfig.default(fakes)
            val azure = AzureTokenGen(config.azure.issuer, config.azure.clientId)

            testApplication {
                application { server(config, h2) }

                val res = jsonHttpClient.post("/intern/saker") {
                    bearerAuth(azure.generate())
                    contentType(ContentType.Application.Json)
                    setBody("{\"personidentifikatorer\":[\"12345678901\"]}")
                }

                assertEquals(HttpStatusCode.OK, res.status)

            }
        }
    }


    private val ApplicationTestBuilder.jsonHttpClient: HttpClient
        get() =
            createClient {
                install(ContentNegotiation) {
                    jackson {
                        registerModule(JavaTimeModule())
                    }
                }
            }
}
