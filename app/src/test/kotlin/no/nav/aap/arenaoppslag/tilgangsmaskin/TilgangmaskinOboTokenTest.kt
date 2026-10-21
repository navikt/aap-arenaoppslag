package no.nav.aap.arenaoppslag.tilgangsmaskin

import com.sun.net.httpserver.HttpServer
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import java.net.InetSocketAddress
import java.net.http.HttpTimeoutException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.measureTime
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import no.nav.aap.arenaoppslag.tilgangsmaskin.TilgangmaskinGatewayImpl.TilgangsmaskinException
import no.nav.aap.arenaoppslag.util.AzureTokenGen
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TilgangmaskinOboTokenTest {
    @Test
    fun `tidsavbrudd og kansellering avbryter lesing fra Texas`(): Unit = runBlocking {
        val avslutt = CountDownLatch(1)
        val antallKall = AtomicInteger()
        val executor = Executors.newCachedThreadPool()
        val texas = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        texas.executor = executor
        texas.createContext("/token/exchange") { exchange ->
            exchange.use {
                it.requestBody.use { body -> body.readBytes() }
                antallKall.incrementAndGet()
                it.responseHeaders.add("Content-Type", "application/json")
                it.sendResponseHeaders(200, 0)
                it.responseBody.write("""{"access_token":"""".toByteArray())
                it.responseBody.flush()
                avslutt.await(10, TimeUnit.SECONDS)
            }
        }
        texas.start()
        val konfig = mapOf(
            "nais.token.exchange.endpoint" to "http://127.0.0.1:${texas.address.port}/token/exchange",
            "integrasjon.tilgangsmaskin.url" to "http://tilgangsmaskin",
            "integrasjon.tilgangsmaskin.scope" to "obo-test-scope",
        )
        val tidligereKonfig = konfig.mapValues { (key, _) -> System.getProperty(key) }
        konfig.forEach { (key, value) -> System.setProperty(key, value) }
        val engine = MockEngine { respond("", HttpStatusCode.NoContent) }
        val brukerToken = OidcToken(AzureTokenGen("issuer", "arenaoppslag", navIdent = "Z123456").generate())
        try {
            TilgangmaskinGatewayImpl(TilgangMaskinHttpClient.lagHttpClient(engine)).use { gateway ->
                val varighet = measureTime {
                    val feil = assertThrows<TilgangsmaskinException> {
                        withTimeout(5_000) { gateway.harTilgangTilPerson("12312312312", brukerToken) }
                    }
                    assertThat(feil).hasRootCauseInstanceOf(HttpTimeoutException::class.java)
                }
                assertThat(varighet.inWholeMilliseconds).isLessThan(4_000)
                assertThat(antallKall.get()).isEqualTo(1)
                assertThat(engine.requestHistory).isEmpty()

                assertThrows<TimeoutCancellationException> {
                    withTimeout(500) { gateway.harTilgangTilPerson("12312312312", brukerToken) }
                }
                assertThat(antallKall.get()).isEqualTo(2)
                assertThat(engine.requestHistory).isEmpty()
            }
        } finally {
            avslutt.countDown()
            engine.close()
            texas.stop(0)
            executor.shutdownNow()
            tidligereKonfig.forEach { (key, value) ->
                if (value == null) System.clearProperty(key) else System.setProperty(key, value)
            }
        }
    }
}
