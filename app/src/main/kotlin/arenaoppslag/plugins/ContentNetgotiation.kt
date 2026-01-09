package arenaoppslag.plugins

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import no.nav.aap.arenaoppslag.kontrakt.ekstern.VedtakResponse

fun Application.contentNegotiation() {
    install(ContentNegotiation) {
        // jackson{DefaultJsonMapper.objectMapper()} // istedenfor
        jackson {
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            registerSubtypes(
                VedtakResponse::class.java // TODO dette er ikke en subtype. Fjernes?
            )
        }
    }
}
