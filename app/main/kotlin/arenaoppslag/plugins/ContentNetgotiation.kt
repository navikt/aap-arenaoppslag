package arenaoppslag.plugins

import arenaoppslag.arenamodell.Vedtak
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import no.nav.aap.arenaoppslag.kontrakt.ekstern.VedtakResponse

fun Application.contentNegotiation() {
    install(ContentNegotiation) {
        jackson {
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            registerSubtypes(
                Vedtak::class.java,
                VedtakResponse::class.java
            )
        }
    }
}
