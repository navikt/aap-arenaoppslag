package no.nav.aap.arenaoppslag.tilgangsmaskin

import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import no.nav.aap.arenaoppslag.DefaultJsonMapper
import org.slf4j.LoggerFactory
import kotlin.math.pow

object TilgangMaskinHttpClient {
    val client = lagHttpClient()

    private val logger = LoggerFactory.getLogger(HttpClient::class.java)

    // Tilgangsmaskinen er tidvis treg eller utilgjengelig. Vi starter derfor med en kort timeout
    // slik at et transient hikk feiler raskt og prøves på nytt, og dobler timeouten for hvert nye
    // forsøk slik at en reelt treg (men fungerende) tjeneste får mer tid før vi til slutt gir opp.
    // Mellom forsøkene venter vi en fast sekund (som tjenesteteamet selv gjør). Verste tilfelle er
    // ca. 1s + 2s + 4s + 8s ventetid på timeout, pluss 1s backoff mellom hvert forsøk.
    private const val MAKS_ANTALL_RETRIES = 3
    private const val INITIELL_TIMEOUT_MS = 1_000L
    private const val MAKS_TIMEOUT_MS = 10_000L
    private const val TIMEOUT_ESKALERINGSFAKTOR = 2.0
    private const val RETRY_DELAY_MS = 1_000L

    private fun eskalertTimeoutMs(retryCount: Int): Long =
        (INITIELL_TIMEOUT_MS * TIMEOUT_ESKALERINGSFAKTOR.pow(retryCount)).toLong().coerceAtMost(MAKS_TIMEOUT_MS)

    fun lagHttpClient(engine: HttpClientEngine = CIO.create()): io.ktor.client.HttpClient = HttpClient(engine) {
        expectSuccess = true // Kaster exception for 4xx og 5xx svar

        // HttpRequestRetry må installeres FØR HttpTimeout for at timeout-exceptions skal fanges og
        // prøves på nytt per forsøk (jf. Ktor-dokumentasjonen for HttpTimeout).
        install(HttpRequestRetry) {
            // Tilgangssjekken er kun lesende, så det er trygt å gjenta POST-kallet.
            // Vi prøver på nytt ved 5xx, transiente nettverksfeil og timeout, men ikke ved 4xx
            // (403 er et svar, ikke en feil) og ikke ved kansellering.
            retryOnServerErrors(maxRetries = MAKS_ANTALL_RETRIES)
            retryOnException(maxRetries = MAKS_ANTALL_RETRIES, retryOnTimeout = true)
            // Fast ett sekund mellom forsøk, likt det tjenesteteamet selv bruker.
            constantDelay(millis = RETRY_DELAY_MS, randomizationMs = 0)
            modifyRequest { request ->
                val timeoutMs = eskalertTimeoutMs(retryCount)
                request.timeout { requestTimeoutMillis = timeoutMs }
                logger.info("Prøver kall mot Tilgangsmaskinen på nytt (forsøk nr. ${retryCount + 1}, timeout $timeoutMs ms)")
            }
        }

        install(HttpTimeout) {
            // Timeout for første forsøk. Økes for hvert nye forsøk, se modifyRequest over.
            requestTimeoutMillis = INITIELL_TIMEOUT_MS
        }

        install(ContentNegotiation) {
            register(
                ContentType.Application.Json,
                JacksonConverter(DefaultJsonMapper.objectMapper()),
            )
        }
    }
}