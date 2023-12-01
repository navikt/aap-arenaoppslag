package arenaoppslag.fellesordningen

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import javax.sql.DataSource

fun Route.fellesordningen(datasource: DataSource) {
    val felleordningRepo = FellesordningenRepo(datasource)

    route("/fellesordningen/vedtak") {
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
