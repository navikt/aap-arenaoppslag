package arenaoppslag.ekstern

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import javax.sql.DataSource

fun Route.ekstern(datasource: DataSource) {
    val eksternRepo = EksternRepo(datasource)

    route("/ekstern") {
        post("/minimum") {
            val request = call.receive<VedtakRequest>()
            call.respond(eksternRepo.hentMinimumLøsning(
                request.personidentifikator,
                request.fraOgMedDato,
                request.tilOgMedDato)
            )
        }
        post("/maksimum") {
            val request = call.receive<VedtakRequest>()
            call.respond(eksternRepo.hentMaksimumsløsning(
                request.personidentifikator,
                request.fraOgMedDato,
                request.tilOgMedDato)
            )
        }
    }
}
