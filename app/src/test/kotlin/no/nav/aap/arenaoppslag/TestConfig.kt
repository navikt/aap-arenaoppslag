package no.nav.aap.arenaoppslag

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.aap.arenaoppslag.util.Fakes
import no.nav.aap.arenaoppslag.util.port
import java.net.URI

internal object TestConfig {
    internal val oracleH2 = DbConfig(
        username = "sa",
        password = "",
        url = "jdbc:h2:mem:request_no;MODE=Oracle;TRACE_LEVEL_SYSTEM_OUT=2",
        driver = "org.h2.Driver"
    )

    fun default(fakes: Fakes): AppConfig {
        return AppConfig(
            proxyUrl = "http://localhost",
            enableProxy = false,
            database = oracleH2,
            azure = AzureConfig(
                jwksUri = URI.create("http://localhost:${fakes.azure.port()}/jwks").toString(),
                issuer = "azure",
                clientId = "no/nav/aap/arenaoppslag"
            )
        )
    }

    val ApplicationTestBuilder.jsonHttpClient: HttpClient
        get() = createClient {
            expectSuccess = true // Kaster exception for 4xx og 5xx svar, altså feiler testen

            install(ContentNegotiation) {
                register(
                    ContentType.Application.Json,
                    JacksonConverter(DefaultJsonMapper.objectMapper())
                )
            }
        }

}
