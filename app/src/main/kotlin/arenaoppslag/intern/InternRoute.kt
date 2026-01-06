package arenaoppslag.intern

import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.arenaoppslag.kontrakt.intern.InternVedtakRequest
import no.nav.aap.arenaoppslag.kontrakt.intern.KanBehandleSoknadIKelvin
import no.nav.aap.arenaoppslag.kontrakt.intern.SakerRequest
import no.nav.aap.komponenter.json.DefaultJsonMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val logger: Logger = LoggerFactory.getLogger("App")

fun Route.person(arenaService: ArenaService) {
    post("/person/aap/eksisterer") {
        logger.info("Sjekker om person eksisterer i AAP-Arena")
        val string = call.receive<String>()
        val request = DefaultJsonMapper.fromJson<SakerRequest>(string)
        val response = arenaService.personEksistererIAapArena(request.personidentifikatorer)

        call.respond(response)
    }
    post("/person/aap/signifikant-historikk") {
        logger.info("Sjekker om personens AAP-Arena-historikk er signifikant for saksbehandling i Kelvin")
        val string = call.receive<String>()
        val request = DefaultJsonMapper.fromJson<KanBehandleSoknadIKelvin>(string) //todo stream
        val response = arenaService.hentRelevanteSakerForPerson(
            request.personidentifikatorer,
            request.virkningstidspunkt
        )

        call.respond(response)
    }
}

fun Route.perioder(arenaService: ArenaService) {
    route("/perioder") {
        post {
            val string = call.receive<String>()
            val request = DefaultJsonMapper.fromJson<InternVedtakRequest>(string)
            val response = arenaService.hentPerioder(
                request.personidentifikator,
                request.fraOgMedDato,
                request.tilOgMedDato
            )

            call.respond(response)
        }
        post("/11-17") {
            val string = call.receive<String>()
            val request = DefaultJsonMapper.fromJson<InternVedtakRequest>(string)
            val response = arenaService.hent11_17Perioder(
                request.personidentifikator,
                request.fraOgMedDato,
                request.tilOgMedDato
            )

            call.respond(response)
        }
    }
}

fun Route.saker(arenaService: ArenaService) {
    post("/saker") {
        logger.info("Henter saker")
        val string = call.receive<String>()
        val request = DefaultJsonMapper.fromJson<SakerRequest>(string)
        val response = arenaService.hentSaker(request.personidentifikatorer)

        call.respond(response)
    }
}

fun Route.maksimum(arenaService: ArenaService) {
    post("/maksimum") {
        logger.info("Henter maksimum")
        val string = call.receive<String>()
        val request = DefaultJsonMapper.fromJson<InternVedtakRequest>(string)
        val response = arenaService.hentMaksimum(
            request.personidentifikator,
            request.fraOgMedDato,
            request.tilOgMedDato
        )

        call.respond(response)
    }
}
