package no.nav.aap.arenaoppslag

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.mockk.Called
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.arenaoppslag.service.PersonService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PersonIdentifikatorApiTest {
    @Test
    fun `ugyldig personidentifikator gir 400 uten personoppslag`() = testApplication {
        val personService = mockk<PersonService>()
        application {
            install(ContentNegotiation) {
                register(
                    ContentType.Application.Json,
                    JacksonConverter(objectMapper = DefaultJsonMapper.objectMapper(), true)
                )
            }
            routing {
                historikk(mockk(), personService)
                sakerForPerson(mockk(), personService)
                maksdato(mockk(), personService)
                samordning(mockk(), personService, mockk())
                vedtakForPerson(mockk(), personService)
                telleverk(mockk(), personService)
                utbetalinger(mockk(), personService)
                manuellFordelingsgrunnlag(mockk(), personService)
            }
        }

        val endepunkter = listOf(
            "/person/historikk/signifikant",
            "/person/historikk",
            "/person/historikk/status/detalj",
            "/person/saker",
            "/person/maksdato",
            "/samordning/person/maksdato",
            "/samordning/meldekort/startuke",
            "/person/vedtak",
            "/person/vedtak/detaljert",
            "/telleverk",
            "/utbetalinger/siste",
            "/manuell-fordelingsgrunnlag",
        )
        endepunkter.forEach { endepunkt ->
            listOf("", "1231231231", "123123123123", "1231231231a").forEach { id ->
                val response = client.post(endepunkt) {
                    contentType(ContentType.Application.Json)
                    setBody("""{"personidentifikator":"$id","virkningstidspunkt":"2026-01-01"}""")
                }

                assertThat(response.status).describedAs(endepunkt).isEqualTo(HttpStatusCode.BadRequest)
            }
        }

        verify { personService wasNot Called }
    }
}
