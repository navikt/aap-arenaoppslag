package arenaoppslag

import arenaoppslag.aap.ArenaService
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.arenaoppslag.kontrakt.intern.InternVedtakRequest
import no.nav.aap.arenaoppslag.kontrakt.intern.KanBehandleSoknadIKelvin
import no.nav.aap.arenaoppslag.kontrakt.intern.PerioderMed11_17Response
import no.nav.aap.arenaoppslag.kontrakt.intern.PersonEksistererIAAPArena
import no.nav.aap.arenaoppslag.kontrakt.intern.PersonHarSignifikantAAPArenaHistorikk
import no.nav.aap.arenaoppslag.kontrakt.intern.SakStatus
import no.nav.aap.arenaoppslag.kontrakt.intern.SakerRequest
import no.nav.aap.arenaoppslag.kontrakt.intern.VedtakResponse
import no.nav.aap.arenaoppslag.kontrakt.modeller.Maksimum

fun Route.perioder(arenaService: ArenaService) {
    route("/perioder") {
        post {
            val request: InternVedtakRequest = call.receive()
            val response: VedtakResponse = arenaService.hentPerioder(
                request.personidentifikator,
                request.fraOgMedDato,
                request.tilOgMedDato
            )

            call.respond(response)
        }
        post("/11-17") {
            val request: InternVedtakRequest = call.receive()
            val response: PerioderMed11_17Response = arenaService.hent11_17Perioder(
                request.personidentifikator,
                request.fraOgMedDato,
                request.tilOgMedDato
            )

            call.respond(response)
        }
    }
}

fun Route.person(arenaService: ArenaService) {
    post("/person/aap/eksisterer") {
        logger.info("Sjekker om person eksisterer i AAP-Arena")
        val request: SakerRequest = call.receive()
        val response: PersonEksistererIAAPArena = arenaService.personEksistererIAapArena(request.personidentifikatorer)

        call.respond(response)
    }
    post("/person/aap/signifikant-historikk") {
        logger.info("Sjekker om personens AAP-Arena-historikk er signifikant for saksbehandling i Kelvin")
        val request: KanBehandleSoknadIKelvin = call.receive()
        val response: PersonHarSignifikantAAPArenaHistorikk = arenaService.signifikanteSakerForPerson(
            request.personidentifikatorer,
            request.virkningstidspunkt
        )

        call.respond(response)
    }
}

fun Route.maksimum(arenaService: ArenaService) {
    post("/maksimum") {
        logger.info("Henter maksimum")
        val request: InternVedtakRequest = call.receive()
        val response: Maksimum = arenaService.hentMaksimum(
            request.personidentifikator,
            request.fraOgMedDato,
            request.tilOgMedDato
        )

        call.respond(response)
    }
}

fun Route.saker(arenaService: ArenaService) {
    post("/saker") {
        logger.info("Henter saker")
        val request: SakerRequest = call.receive()
        val response: List<SakStatus> = arenaService.hentSaker(request.personidentifikatorer)

        call.respond(response)
    }
}
