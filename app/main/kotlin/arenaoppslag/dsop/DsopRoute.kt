package arenaoppslag.dsop

import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.arenaoppslag.kontrakt.dsop.DsopRequest
import javax.sql.DataSource

fun Route.dsop(datasource: DataSource) {
    val dsopRepo = DsopRepo(datasource)

    route("/dsop") {
        post("/vedtak") {
            val request = call.receive<DsopRequest>()
            call.respond(
                dsopRepo.hentVedtak(
                    request.personId,
                    request.periode,
                    request.samtykkePeriode
                ).tilKontrakt()
            )
        }
        post("/meldekort") {
            val request = call.receive<DsopRequest>()
            call.respond(
                dsopRepo.hentMeldeplikt(
                    request.personId,
                    request.periode,
                    request.samtykkePeriode
                ).tilKontrakt()
            )
        }
    }
}
