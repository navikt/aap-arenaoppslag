package arenaoppslag

import arenaoppslag.fellesordningen.FellesordningenRepo
import arenaoppslag.fellesordningen.VedtakRequest
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.sql.DataSource


private val secureLog: Logger = LoggerFactory.getLogger("secureLog")

fun Route.testroute (datasource: DataSource) {
    val felleordningRepo = FellesordningenRepo(datasource)

    route("/intern/test") {
        post {
            val request = call.receive<VedtakRequest>()
            secureLog.info("mottatt test kall til api")
            call.respond(felleordningRepo.selectMaksimumsløsning(
                request.personidentifikator,
                request.fraOgMedDato,
                request.tilOgMedDato)
            ).also {
                secureLog.info("respons fra arenaoppslag: ${this.context.response.toString()}")
            }
        }
    }
}