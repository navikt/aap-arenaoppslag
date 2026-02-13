package no.nav.aap.arenaoppslag

import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.arenaoppslag.kontrakt.intern.InternVedtakRequest
import no.nav.aap.arenaoppslag.kontrakt.intern.PerioderMed11_17Response
import no.nav.aap.arenaoppslag.kontrakt.intern.SakStatus
import no.nav.aap.arenaoppslag.kontrakt.intern.SakerRequest
import no.nav.aap.arenaoppslag.kontrakt.intern.VedtakResponse
import no.nav.aap.arenaoppslag.kontrakt.modeller.Maksimum

fun Route.perioder(internService: InternService) {
    route("/perioder") {
        post {
            val request: InternVedtakRequest = call.receive()
            val response: VedtakResponse = internService.hentPerioder(
                request.personidentifikator,
                request.fraOgMedDato,
                request.tilOgMedDato
            )

            call.respond(response)
        }
        post("/11-17") {
            val request: InternVedtakRequest = call.receive()
            val response: PerioderMed11_17Response = internService.hent11_17Perioder(
                request.personidentifikator,
                request.fraOgMedDato,
                request.tilOgMedDato
            )

            call.respond(response)
        }
    }
}

fun Route.maksimum(internService: InternService) {
    post("/maksimum") {
        logger.info("Henter maksimum")
        val request: InternVedtakRequest = call.receive()
        val response: Maksimum = internService.hentMaksimum(
            request.personidentifikator,
            request.fraOgMedDato,
            request.tilOgMedDato
        )

        call.respond(response)
    }
}

fun Route.saker(internService: InternService) {
    post("/saker") {
        logger.info("Henter saker")
        val request: SakerRequest = call.receive()
        val response: List<SakStatus> = internService.hentSaker(request.personidentifikatorer)

        call.respond(response)
    }
}
