package arenaoppslag.intern

import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.arenaoppslag.kontrakt.intern.InternVedtakRequest
import no.nav.aap.arenaoppslag.kontrakt.intern.PerioderMed11_17Response
import no.nav.aap.arenaoppslag.kontrakt.intern.PersonEksistererIAAPArena
import no.nav.aap.arenaoppslag.kontrakt.intern.SakerRequest
import no.nav.aap.komponenter.json.DefaultJsonMapper
import org.slf4j.LoggerFactory
import javax.sql.DataSource
import no.nav.aap.arenaoppslag.kontrakt.intern.VedtakResponse as KontraktVedtakResponse

val logger = LoggerFactory.getLogger("App")

fun Route.intern(datasource: DataSource) {
    val internRepo = InternRepo(datasource)

    route("/intern") {
        route("/perioder") {
            post {
                val string = call.receive<String>()
                val request = DefaultJsonMapper.fromJson<InternVedtakRequest>(string)
                call.respond(
                    KontraktVedtakResponse(
                        perioder = internRepo.hentPerioder(
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
            val string = call.receive<String>()
            val request = DefaultJsonMapper.fromJson<SakerRequest>(string)
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
            val string = call.receive<String>()
            val request = DefaultJsonMapper.fromJson<InternVedtakRequest>(string)
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
            val string = call.receive<String>()
            val request = DefaultJsonMapper.fromJson<SakerRequest>(string)
            val saker = request.personidentifikatorer.flatMap { personidentifikator ->
                internRepo.hentSaker(personidentifikator)
            }.distinctBy { it.sakId }
            call.respond(saker)
        }
    }
}


