package no.nav.aap.arenaoppslag

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.aap.arenaoppslag.modeller.SakId
import no.nav.aap.arenaoppslag.modeller.Saksnummer
import no.nav.aap.arenaoppslag.service.MigreringService
import no.nav.aap.arenaoppslag.service.SakService

fun Route.migrering(sakService: SakService, migreringService: MigreringService) {
    get("/{saksnummer}/krav") {
        val saksnummer = Saksnummer.fromString(call.parameters["saksnummer"])

        if (saksnummer == null) {
            logger.info("saksnummer er på et ugyldig format")
            return@get call.respond(HttpStatusCode.BadRequest)
        }

        val sak = sakService.hentSak(saksnummer)

        if (sak == null) {
            logger.info("Fant ikke sak med saksnummer $saksnummer")
            return@get call.respond(HttpStatusCode.NotFound)
        }

        val sakId = sak.sakId.toIntOrNull()?.let { SakId(it) }

        if (sakId == null) {
            logger.info("Fant ikke gyldig sak_id for sak med saksnummer $saksnummer")
            return@get call.respond(HttpStatusCode.NotFound)
        }

        logger.info("Henter ut relevant migreringsinformasjon for krav-steget for sak")
        val krav = migreringService.hentKravForSak(sak, sakId)
        call.respond(status = HttpStatusCode.OK, message = krav.tilKontrakt())
    }
}
