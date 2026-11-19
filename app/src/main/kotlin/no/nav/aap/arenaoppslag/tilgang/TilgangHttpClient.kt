package no.nav.aap.arenaoppslag.tilgang

import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import no.nav.aap.arenaoppslag.DefaultJsonMapper
import kotlin.time.Duration.Companion.seconds

object TilgangHttpClient {
    fun lagHttpClient(engine: HttpClientEngine = CIO.create()): HttpClient = HttpClient(engine) {
        expectSuccess = true
        install(HttpRequestRetry)
        install(HttpTimeout) {
            socketTimeoutMillis = 5.seconds.inWholeMilliseconds
            connectTimeoutMillis = 2.seconds.inWholeMilliseconds
            requestTimeoutMillis = 5.seconds.inWholeMilliseconds
        }
        install(ContentNegotiation) {
            register(
                ContentType.Application.Json,
                JacksonConverter(DefaultJsonMapper.objectMapper()),
            )
        }
    }
}
