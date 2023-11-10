package arenaoppslag.dsop

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import javax.sql.DataSource

fun Route.dsop(datasource: DataSource) {
    val dsopRepo = DsopRepo(datasource)

    route("/dsop") {
        post("/vedtak") {
            val request = call.receive<DsopRequest>()
            call.respond(dsopRepo.hentVedtak(request.personId, request.periode, request.samtykkePeriode))
        }
        post("/meldekort") {
            val request = call.receive<DsopRequest>()
            call.respond(dsopRepo.hentMeldeplikt(request.personId, request.periode, request.samtykkePeriode))
        }
    }
}
