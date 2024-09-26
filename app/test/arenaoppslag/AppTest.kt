package arenaoppslag

import arenaoppslag.ekstern.VedtakResponse
import arenaoppslag.modeller.VedtakRequest
import arenaoppslag.util.AzureTokenGen
import arenaoppslag.util.Fakes
import arenaoppslag.util.H2TestBase
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

class AppTest : H2TestBase() {

    @Test
    fun `Henter ut vedtak for fellesordningen`() {
        Fakes().use { fakes ->
            val config = TestConfig.default(fakes)
            val azure = AzureTokenGen(config.azure)

            testApplication {
                application { server(config, h2) }

                val res = jsonHttpClient.post("/ekstern/minimum") {
                    bearerAuth(azure.generate())
                    contentType(ContentType.Application.Json)
                    setBody(
                        VedtakRequest(
                            personidentifikator = "1",
                            fraOgMedDato = LocalDate.of(2022, 10, 1),
                            tilOgMedDato = LocalDate.of(2023, 12, 31)
                        )
                    )
                }

                assertEquals(HttpStatusCode.OK, res.status)

                val alleVedtak = res.body<VedtakResponse>()

                assertEquals(1, alleVedtak.perioder.size)
            }
        }
    }

    private val ApplicationTestBuilder.jsonHttpClient: HttpClient
        get() =
            createClient {
                install(ContentNegotiation) { jackson {
                    registerModule(JavaTimeModule())
                } }
            }
}
