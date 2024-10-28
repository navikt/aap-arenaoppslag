package arenaoppslag.intern

import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.arenaoppslag.kontrakt.intern.InternVedtakRequest
import no.nav.aap.arenaoppslag.kontrakt.intern.PerioderMed11_17Response
import no.nav.aap.arenaoppslag.kontrakt.intern.SakerRequest
import javax.sql.DataSource
import no.nav.aap.arenaoppslag.kontrakt.intern.VedtakResponse as KontraktVedtakResponse


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
        post("/maksimum") {
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
            val request = call.receive<SakerRequest>()
            call.respond(
                internRepo.hentSaker(
                    request.personidentifikator
                )
            )
        }
    }
}


