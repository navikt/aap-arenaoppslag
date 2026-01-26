package arenaoppslag

import arenaoppslag.util.Fakes
import arenaoppslag.util.port
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import no.nav.aap.arenaoppslag.server

fun main() {
    val fakes = Fakes()

    val config = TestConfig.default(fakes)
    println("Azure port: ${fakes.azure.port()}")

    embeddedServer(Netty, port = 8080) {
        server(
            config = config,
        )
        module()
    }.start(wait = true)
}

private fun Application.module() {
    monitor.subscribe(ApplicationStopped) { application ->
        application.environment.log.info("Server har stoppet")
        application.monitor.unsubscribe(ApplicationStopped) {}
    }
}
