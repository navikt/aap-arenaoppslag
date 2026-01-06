package arenaoppslag.intern

import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.arenaoppslag.kontrakt.intern.*
import no.nav.aap.komponenter.json.DefaultJsonMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.sql.DataSource
import no.nav.aap.arenaoppslag.kontrakt.intern.VedtakResponse as KontraktVedtakResponse

val logger: Logger = LoggerFactory.getLogger("App")

fun Route.intern(datasource: DataSource) {
    val arenaRepository = ArenaRepository(datasource)
    val relevantHistorikkService = RelevantHistorikkService(arenaRepository)

    route("/intern") {
        route("/perioder") {
            post {
                val string = call.receive<String>()
                val request = DefaultJsonMapper.fromJson<InternVedtakRequest>(string)
                call.respond(
                    KontraktVedtakResponse(
                        perioder = arenaRepository.hentPerioder(
                            request.personidentifikator,
                            request.fraOgMedDato,
                            request.tilOgMedDato
                        ).map { it.tilKontrakt() }
                    )
                )
            }
            post("/11-17") {
                val string = call.receive<String>()
                val request = DefaultJsonMapper.fromJson<InternVedtakRequest>(string)
                call.respond(
                    PerioderMed11_17Response(
                        perioder = arenaRepository.hentPeriodeInkludert11_17(
                            request.personidentifikator,
                            request.fraOgMedDato,
                            request.tilOgMedDato
                        ).map { it.tilKontrakt() }
                    )
                )
            }
        }
        post("/person/aap/eksisterer") {
            logger.info("Sjekker om person eksisterer i AAP-Arena")
            val string = call.receive<String>()
            val request = DefaultJsonMapper.fromJson<SakerRequest>(string)
            call.respond(
                PersonEksistererIAAPArena(
                    request.personidentifikatorer.map { personidentifikator ->
                        arenaRepository.hentEksistererIAAPArena(personidentifikator)
                    }.any { it.equals(true) } // todo simplify
                )
            )
        }
        post("/person/aap/signifikant-historikk") {
            logger.info("Sjekker om personens AAP-Arena-historikk er signifikant for saksbehandling i Kelvin")
            val string = call.receive<String>()
            val request = DefaultJsonMapper.fromJson<KanBehandleSoknadIKelvin>(string) //todo stream
            val response = relevantHistorikkService.hentRelevanteSakerForPerson(
                request.personidentifikatorer, request.virkningstidspunkt
            )
            call.respond(response)
        }
        post("/maksimum") {
            logger.info("Henter maksimum")
            val string = call.receive<String>()
            val request = DefaultJsonMapper.fromJson<InternVedtakRequest>(string)
            call.respond(
                arenaRepository.hentMaksimumsløsning(
                    request.personidentifikator,
                    request.fraOgMedDato,
                    request.tilOgMedDato
                ).tilKontrakt()
            )
        }
        post("/saker") {
            logger.info("Henter saker")
            val string = call.receive<String>()
            val request = DefaultJsonMapper.fromJson<SakerRequest>(string)
            val saker = request.personidentifikatorer.flatMap { personidentifikator ->
                arenaRepository.hentSaker(personidentifikator)
            }
            call.respond(saker)
        }
    }
}


