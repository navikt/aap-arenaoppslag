package no.nav.aap.arenaoppslag

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.arenaoppslag.kontrakt.intern.PersonEksistererIAAPArena
import no.nav.aap.arenaoppslag.kontrakt.intern.SakerRequest
import no.nav.aap.arenaoppslag.kontrakt.intern.SignifikanteSakerRequest
import no.nav.aap.arenaoppslag.kontrakt.intern.SignifikanteSakerResponse
import no.nav.aap.arenaoppslag.kontrakt.intern.TellerRequest
import no.nav.aap.arenaoppslag.modeller.ArenaSakDetaljertRespons
import no.nav.aap.arenaoppslag.service.HistorikkService
import no.nav.aap.arenaoppslag.service.InternService
import no.nav.aap.arenaoppslag.service.TelleverkService

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


fun Route.sak(sakOgVedtakService: SakOgVedtakService, telleverkService: TelleverkService ) {
    get("/sak/{sakid}/detaljert") {
        val sakid = call.parameters["sakid"]?.toIntOrNull()

        if (sakid == null) {
            logger.info("Sakid kan ikke være NULL eller et ugyldig tall")
            return@get call.respond(HttpStatusCode.BadRequest)
        }

        when(val sak = sakOgVedtakService.hentSakMedVedtak(sakid)) {
            null -> call.respond(status = HttpStatusCode.NotFound, message = "Fant ikke sak")
            else -> {
                val fodselsnr = sak.fodselsnummer
                val telleverk  = telleverkService.hentTelleverkPåPerson(fodselsnr)
                val response = ArenaSakDetaljertRespons.fromDomain(sak, telleverk)

                call.respond(status = HttpStatusCode.OK, message = response)
            }
        }

    }
}

fun Route.telleverk(telleverkService: TelleverkService) {
    post("/telleverk") {
        logger.info("Henter telleverk")
        val request: TellerRequest = call.receive()
        val fodselsnummer = request.personidentifikator

        //TODO BRUK PDL for å finne andre personidentifikatorer knyttet til samme person

        when(val telleverk  = telleverkService.hentTelleverkPåPerson(fodselsnummer)) {
            null -> call.respond(status = HttpStatusCode.NotFound, message = "Fant ikke telleverk for person")
            else -> call.respond(status = HttpStatusCode.OK, message = telleverk)
        }

    }
}