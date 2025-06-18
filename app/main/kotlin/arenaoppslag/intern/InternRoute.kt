package arenaoppslag.intern

import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.arenaoppslag.kontrakt.intern.*
import javax.sql.DataSource
import org.slf4j.LoggerFactory
import no.nav.aap.arenaoppslag.kontrakt.intern.VedtakResponse as KontraktVedtakResponse

val logger = LoggerFactory.getLogger("App")

fun Route.intern(datasource: DataSource) {
    val internRepo = InternRepo(datasource)

    route("/intern") {
        route("/perioder") {
            post {
                val request = call.receive<InternVedtakRequest>()
                call.respond(
                    KontraktVedtakResponse(
                        perioder = internRepo.hentMinimumLøsning(
                            request.personidentifikator,
                            request.fraOgMedDato,
                            request.tilOgMedDato
                        ).map { it.tilKontrakt() }
                    )
                )
            }
            post("/11-17") {
                val request = call.receive<InternVedtakRequest>()
                call.respond(
                    PerioderMed11_17Response(
                        perioder = internRepo.hentPeriodeInkludert11_17(
                            request.personidentifikator,
                            request.fraOgMedDato,
                            request.tilOgMedDato
                        ).map { it.tilKontrakt() }
                    )
                )
            }
        }
        post("/person/aap/eksisterer") {
            logger.info("Sjekker om person eksisterer")
            val request = call.receive<SakerRequest>()
            call.respond(
                PersonEksistererIAAPArena(
                    request.personidentifikatorer.map { personidentifikator ->
                        internRepo.hentEksistererIAAPArena(personidentifikator)
                    }.any { it.equals(true) }
                )
            )
        }
        post("/maksimum") {
            logger.info("Henter maksimum")
            val request = call.receive<InternVedtakRequest>()
            call.respond(
                internRepo.hentMaksimumsløsning(
                    request.personidentifikator,
                    request.fraOgMedDato,
                    request.tilOgMedDato
                ).tilKontrakt()
            )
        }
        post("/saker") {
            logger.info("Henter saker")
            val request = call.receive<SakerRequest>()
            val saker = request.personidentifikatorer.flatMap { personidentifikator ->
                internRepo.hentSaker(personidentifikator)
            }
            call.respond(saker)
        }
    }
}


