package arenaoppslag.intern

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import javax.sql.DataSource

fun Route.intern(datasource: DataSource) {
    val internRepo = InternRepo(datasource)

    route("/intern") {
        route("/perioder"){
            post {
                val request = call.receive<VedtakRequest>()
                call.respond(internRepo.hentMinimumLøsning(
                    request.personidentifikator,
                    request.fraOgMedDato,
                    request.tilOgMedDato)
                )
            }
            post("/11-17") {
                val request = call.receive<VedtakRequest>()
                call.respond(internRepo.hentPeriodeInkludert11_17(
                    request.personidentifikator,
                    request.fraOgMedDato,
                    request.tilOgMedDato)
                )
            }
        }
        post("/maksimum") {
            val request = call.receive<VedtakRequest>()
            call.respond(internRepo.hentMaksimumsløsning(
                request.personidentifikator,
                request.fraOgMedDato,
                request.tilOgMedDato)
            )
        }
        post("/saker") {
            val request = call.receive<SakerRequest>()
            call.respond(internRepo.hentSaker(
                request.personidentifikator
            )
            )
        }
    }
}


