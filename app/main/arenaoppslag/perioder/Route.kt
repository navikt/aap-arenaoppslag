package arenaoppslag.perioder

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import javax.sql.DataSource

fun Route.perioder(datasource: DataSource) {
    val felleordningRepo = PerioderRepo(datasource)

    route("/intern/perioder") {
        post {
            val request = call.receive<PerioderRequest>()
            call.respond(felleordningRepo.hentGrunnInfoForAAPMotaker(
                request.personidentifikator,
                request.fraOgMedDato,
                request.tilOgMedDato)
            )
        }
    }
}
