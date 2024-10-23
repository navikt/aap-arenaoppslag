package arenaoppslag

import arenaoppslag.ekstern.EksternRepo
import arenaoppslag.modeller.VedtakRequest
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.sql.DataSource


private val secureLog: Logger = LoggerFactory.getLogger("secureLog")

fun Route.testroute (datasource: DataSource) {
    val eksternRepo = EksternRepo(datasource)

    route("/intern/maksimum") {
        post {
            val request = call.receive<VedtakRequest>()
            secureLog.info("mottatt test kall til api")
            val res = eksternRepo.hentMaksimumsløsning(
                request.personidentifikator,
                request.fraOgMedDato,
                request.tilOgMedDato)
            call.respond(res
            ).also {
                secureLog.info("respons fra arenaoppslag: $res")
            }
        }
    }
}