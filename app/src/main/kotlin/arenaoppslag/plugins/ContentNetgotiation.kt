package arenaoppslag.plugins

import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import no.nav.aap.komponenter.json.DefaultJsonMapper

fun Application.contentNegotiation() {
    install(ContentNegotiation) {
        register(
            ContentType.Application.Json,
            JacksonConverter(DefaultJsonMapper.objectMapper())
        )
    }
}
