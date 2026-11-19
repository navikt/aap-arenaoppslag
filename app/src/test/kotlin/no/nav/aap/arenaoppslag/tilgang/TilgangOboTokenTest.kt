package no.nav.aap.arenaoppslag.tilgang

import io.ktor.client.engine.mock.MockEngine
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import no.nav.aap.arenaoppslag.modeller.PersonIdentifikator
import no.nav.aap.arenaoppslag.util.AzureTokenGen
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.TokenProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.time.measureTime

class TilgangOboTokenTest {
    @Test
    fun `kansellering avbryter blokkerende tokenprovider`(): Unit = runBlocking {
        val startet = CountDownLatch(1)
        val avbrutt = CountDownLatch(1)
        val tokenProvider = object : TokenProvider {
            override fun getToken(scope: String?, currentToken: OidcToken?): OidcToken? {
                startet.countDown()
                try {
                    CountDownLatch(1).await()
                } finally {
                    avbrutt.countDown()
                }
                error("Skal avbrytes")
            }
        }
        val konfig = mapOf(
            "integrasjon.tilgang.url" to "http://tilgang",
            "integrasjon.tilgang.scope" to "obo-test-scope",
        )
        val tidligereKonfig = konfig.mapValues { (key, _) -> System.getProperty(key) }
        konfig.forEach { (key, value) -> System.setProperty(key, value) }
        val brukerToken = OidcToken(AzureTokenGen("issuer", "arenaoppslag").generate())
        val engine = MockEngine { error("Skal ikke kalle tilgang") }
        try {
            TilgangGatewayImpl(TilgangHttpClient.lagHttpClient(engine), tokenProvider).use { gateway ->
                val varighet = measureTime {
                    assertThrows<TimeoutCancellationException> {
                        withTimeout(1_000) {
                            gateway.harTilgangTilPerson(PersonIdentifikator("12312312312"), brukerToken)
                        }
                    }
                }
                assertThat(startet.count).isZero()
                assertThat(avbrutt.await(1, TimeUnit.SECONDS)).isTrue()
                assertThat(varighet.inWholeMilliseconds).isLessThan(3_000)
                assertThat(engine.requestHistory).isEmpty()
            }
        } finally {
            engine.close()
            tidligereKonfig.forEach { (key, value) ->
                if (value == null) System.clearProperty(key) else System.setProperty(key, value)
            }
        }
    }
}
