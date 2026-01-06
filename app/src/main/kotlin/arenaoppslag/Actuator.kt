package arenaoppslag

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

fun Route.actuator(prometheus: PrometheusMeterRegistry) {
    route("/actuator") {
        get("/metrics") {
            call.respond(prometheus.scrape())
        }
        get("/live") {
            call.respond(HttpStatusCode.OK, "arenaoppslag")
        }
        get("/ready") {
            call.respond(HttpStatusCode.OK, "arenaoppslag")
        }
    }
}
