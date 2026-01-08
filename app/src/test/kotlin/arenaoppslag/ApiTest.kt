package arenaoppslag

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
import no.nav.aap.arenaoppslag.kontrakt.ekstern.EksternVedtakRequest
import no.nav.aap.arenaoppslag.kontrakt.ekstern.VedtakResponse
import no.nav.aap.arenaoppslag.kontrakt.modeller.Maksimum
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ApiTest : H2TestBase() {

    private lateinit var tokenProvider: AzureTokenGen
    private lateinit var config: AppConfig

    @BeforeEach
    fun setup() {
        config = TestConfig.default(Fakes())
        tokenProvider = AzureTokenGen(config.azure.issuer, config.azure.clientId)
    }


    @Test
    fun `Henter ut vedtak for fellesordningen`() {
        testApplication {
            application { server(config, h2) }

            val res = jsonHttpClient.post("/intern/perioder") {
                bearerAuth(tokenProvider.generate())
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


    @Test
    fun `Henter ut maksimumsvedtak for fellesordningen`() {
        testApplication {
            application { server(config, h2) }

            val res = jsonHttpClient.post("/intern/maksimum") {
                bearerAuth(tokenProvider.generate())
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


    @Test
    fun `Henter ut saker by fnr`() {
        testApplication {
            application { server(config, h2) }

            val res = jsonHttpClient.post("/intern/saker") {
                bearerAuth(tokenProvider.generate())
                contentType(ContentType.Application.Json)
                setBody("{\"personidentifikatorer\":[\"12345678901\"]}")
            }

            assertEquals(HttpStatusCode.OK, res.status)

        }
    }


    private val ApplicationTestBuilder.jsonHttpClient: HttpClient
        get() = createClient {
            install(ContentNegotiation) {
                jackson {
                    registerModule(JavaTimeModule())
                }
            }
        }
}
