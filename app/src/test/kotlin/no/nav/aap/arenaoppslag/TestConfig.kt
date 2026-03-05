package no.nav.aap.arenaoppslag

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.testing.*
import no.nav.aap.arenaoppslag.util.Fakes
import no.nav.aap.arenaoppslag.util.port
import java.io.File
import java.net.URI

internal object TestConfig {

    private val dbPath = File("build/test-db/request_no").absolutePath

    internal val oracleH2 = DbConfig(
        username = "SA",
        password = "",
        url = "jdbc:h2:file:$dbPath;MODE=Oracle;AUTO_SERVER=TRUE;IFEXISTS=FALSE",
        driver = "org.h2.Driver"
    )

    internal val oracleH2InMem = DbConfig(
        username = "SA",
        password = "",
        url = "jdbc:h2:mem:request_no;MODE=Oracle;TRACE_LEVEL_SYSTEM_OUT=1", // 1=ERROR, 2=INFO
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
