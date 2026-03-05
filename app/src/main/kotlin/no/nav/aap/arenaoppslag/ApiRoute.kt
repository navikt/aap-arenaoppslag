package no.nav.aap.arenaoppslag

import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.arenaoppslag.kontrakt.intern.PersonEksistererIAAPArena
import no.nav.aap.arenaoppslag.kontrakt.intern.SakerRequest
import no.nav.aap.arenaoppslag.kontrakt.intern.SignifikanteSakerRequest
import no.nav.aap.arenaoppslag.kontrakt.intern.SignifikanteSakerResponse

fun Route.historikk(historikkService: HistorikkService) {

    post("/person/signifikant-historikk") {
        logger.info("Sjekker om personens AAP-Arena-historikk er signifikant for saksbehandling i Kelvin")
        val request: SignifikanteSakerRequest = call.receive()
        val response: SignifikanteSakerResponse = historikkService.signifikanteSakerForPerson(
            request.personidentifikatorer.toSet(), request.virkningstidspunkt
        )

        call.respond(response)
    }

    post("/person/eksisterer") {
        logger.info("Sjekker om person eksisterer i AAP-Arena")
        val request: SakerRequest = call.receive()
        val response: PersonEksistererIAAPArena =
            historikkService.personEksistererIAapArena(request.personidentifikatorer.toSet())

        call.respond(response)
    }

}
