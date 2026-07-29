package no.nav.aap.arenaoppslag

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.testing.ApplicationTestBuilder
import java.io.File

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

    fun default(): AppConfig {
        return AppConfig(
            proxyUrl = "http://localhost",
            enableProxy = false,
            database = oracleH2,
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
