package arenaoppslag.ekstern

import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.arenaoppslag.kontrakt.ekstern.EksternVedtakRequest
import no.nav.aap.arenaoppslag.kontrakt.ekstern.VedtakResponse
import javax.sql.DataSource

fun Route.ekstern(datasource: DataSource) {
    val eksternRepo = EksternRepo(datasource)

    route("/ekstern") {
        post("/minimum") {
            val request = call.receive<EksternVedtakRequest>()
            call.respond(
                VedtakResponse(
                    perioder = eksternRepo.hentMinimumLøsning(
                        request.personidentifikator,
                        request.fraOgMedDato,
                        request.tilOgMedDato
                    ).map { it.tilKontrakt() }
                )
            )
        }
        post("/maksimum") {
            val request = call.receive<EksternVedtakRequest>()
            call.respond(
                eksternRepo.hentMaksimumsløsning(
                    request.personidentifikator,
                    request.fraOgMedDato,
                    request.tilOgMedDato
                ).tilKontrakt()
            )
        }
    }
}
