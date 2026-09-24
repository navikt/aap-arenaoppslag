package no.nav.aap.arenaoppslag

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import no.nav.aap.arenaoppslag.modeller.ArenaSak
import no.nav.aap.arenaoppslag.modeller.SakId
import no.nav.aap.arenaoppslag.modeller.Saksnummer
import no.nav.aap.arenaoppslag.service.MigreringService
import no.nav.aap.arenaoppslag.service.SakService

fun Route.migrering(sakService: SakService, migreringService: MigreringService) {
    get("/{saksnummer}/krav") {
        val (sak, sakId) = hentSakFraSaksnummer(sakService) ?: return@get

        logger.info("Henter ut relevant migreringsinformasjon for krav-steget for sak")
        val krav = migreringService.hentKravForSak(sak, sakId)
        call.respond(status = HttpStatusCode.OK, message = krav.tilKontrakt())
    }

    get("/{saksnummer}/sykdom") {
        val (_, sakId) = hentSakFraSaksnummer(sakService) ?: return@get

        logger.info("Henter ut relevant migreringsinformasjon for sykdom-steget for sak")
        val sykdomsvurdering = migreringService.hentSykdomsvurderingForSak(sakId)

        if (sykdomsvurdering == null) {
            logger.info("Fant ikke gjeldende 11-5-vedtak for sak")
            return@get call.respond(HttpStatusCode.NotFound)
        }

        call.respond(status = HttpStatusCode.OK, message = sykdomsvurdering.tilKontrakt())
    }
}

// Svarer selv med 400/404 og returnerer null når saken ikke kan slås opp, så route-en bare avbryter
private suspend fun RoutingContext.hentSakFraSaksnummer(sakService: SakService): Pair<ArenaSak, SakId>? {
    val saksnummer = Saksnummer.fromString(call.parameters["saksnummer"])

    if (saksnummer == null) {
        logger.info("saksnummer er på et ugyldig format")
        call.respond(HttpStatusCode.BadRequest)
        return null
    }

    val sak = sakService.hentSak(saksnummer)

    if (sak == null) {
        logger.info("Fant ikke sak med saksnummer $saksnummer")
        call.respond(HttpStatusCode.NotFound)
        return null
    }

    val sakId = sak.sakId.toIntOrNull()?.let { SakId(it) }

    if (sakId == null) {
        logger.info("Fant ikke gyldig sak_id for sak med saksnummer $saksnummer")
        call.respond(HttpStatusCode.NotFound)
        return null
    }

    return sak to sakId
}
