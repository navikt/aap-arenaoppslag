package arenaoppslag

import arenaoppslag.fellesordningen.FellesordningenRepo
import arenaoppslag.fellesordningen.VedtakRequest
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import javax.sql.DataSource

fun Route.testroute (datasource: DataSource) {
    val felleordningRepo = FellesordningenRepo(datasource)

    route("/intern/test") {
        post {
            val request = call.receive<VedtakRequest>()
            call.respond(felleordningRepo.hentGrunnInfoForAAPMotaker(
                request.personidentifikator,
                request.fraOgMedDato,
                request.tilOgMedDato)
            )
        }
    }
}