package no.nav.aap.arenaoppslag

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.arenaoppslag.modeller.PersonId
import no.nav.aap.arenaoppslag.modeller.Saksnummer
import no.nav.aap.arenaoppslag.modeller.TelleverkResponse
import no.nav.aap.arenaoppslag.service.OppgaveService
import no.nav.aap.arenaoppslag.service.PosteringService
import no.nav.aap.arenaoppslag.service.SakService
import no.nav.aap.arenaoppslag.service.SaksopplysningService
import no.nav.aap.arenaoppslag.service.TelleverkService
import no.nav.aap.arenaoppslag.service.TilkjentYtelserService

/**
 * Ting som ligger i denne fila skal ligge under /api/intern
 * og er mest ment til visningsklienten som gir detaljert innsyn i arenasaker
 */
fun Route.sakDetaljert(
    sakOgVedtakService: SakOgVedtakService,
    saksopplysningService: SaksopplysningService,
) {
    get("/sak/{saksnummer}/detaljert") {
        val saksnummer = Saksnummer.fromString(call.parameters["saksnummer"])

        if (saksnummer == null) {
            logger.info("saksnummer er på et ugyldig format")
            return@get call.respond(HttpStatusCode.BadRequest)
        }

        val sak = sakOgVedtakService.hentSakMedVedtak(saksnummer)

        if (sak == null) {
            logger.info("Klarte ikke hente sak for saksnummer $saksnummer")
            return@get call.respond(HttpStatusCode.NotFound)
        }

        val saksopplysningerPerVedtak = saksopplysningService.hentForVedtakIder(sak.vedtak.map { it.vedtakId })
        val alleSaksopplysninger = sak.vedtak.associate { vedtak ->
            vedtak.vedtakId to (saksopplysningerPerVedtak[vedtak.vedtakId] ?: emptyList())
        }
        val samordningPerVedtak = saksopplysningService.hentSamordningOgInstitusjon(alleSaksopplysninger)
        val sakMedSamordning = sak.copy(
            vedtak = sak.vedtak.map { vedtak -> vedtak.medSamordning(samordningPerVedtak[vedtak.vedtakId]) }
        )


        logger.info("Henter saksdetaljer")
        val response = sakMedSamordning.tilKontrakt()
        call.respond(status = HttpStatusCode.OK, message = response)
    }
}

fun Route.tilkjentYtelseForSak(sakService: SakService, tilkjentYtelserService: TilkjentYtelserService) {
    get("/sak/{saksnummer}/tilkjent-ytelse") {
        val saksnummer = Saksnummer.fromString(call.parameters["saksnummer"])

        if (saksnummer == null) {
            logger.info("saksnummer er på et ugyldig format")
            return@get call.respond(HttpStatusCode.BadRequest)
        }

        val sakId = sakService.hentSakId(saksnummer)

        if (sakId == null) {
            logger.info("Fant ikke sak med saksnummer $saksnummer")
            return@get call.respond(HttpStatusCode.NotFound)
        }

        logger.info("Henter tilkjent ytelse for sak")
        // En sak uten meldekort og posteringer gir en tom rad-liste, ikke 404
        val tilkjentYtelse = tilkjentYtelserService.hentTilkjenteYtelserForSak(sakId)
        call.respond(status = HttpStatusCode.OK, message = tilkjentYtelse)
    }
}

fun Route.kvotehistorikkForSak(sakService: SakService, telleverkService: TelleverkService) {
    get("/sak/{saksnummer}/kvotehistorikk") {
        logger.info("Henter kvotehistorikk for sak")
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

        val kvoteHistorikk = telleverkService.hentKvoteBrukHendelserForPerson(PersonId(sak.person.personId))

        call.respond(status = HttpStatusCode.OK, message = kvoteHistorikk)
    }
}

fun Route.oppgaverForSak(sakService: SakService, oppgaveService: OppgaveService) {
    get("/sak/{saksnummer}/oppgaver") {
        logger.info("Henter oppgaver for sak")
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

        val oppgaver = oppgaveService.hentOppgaverForPerson(PersonId(sak.person.personId))

        call.respond(status = HttpStatusCode.OK, message = oppgaver)
    }
}

fun Route.telleverkForSak(sakService: SakService, posteringService: PosteringService, telleverkService: TelleverkService) {
    get("/sak/{saksnummer}/telleverk") {
        logger.info("Henter telleverk for sak")
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

        val personId = PersonId(sak.person.personId)

        val tellerverk = telleverkService.hentTelleverkForPerson(personId)
        val maksdato = sakService.hentMaksdatoAapForPerson(personId)
        val sisteUtbetalingDato = posteringService.hentSisteAapUtbetalingForPerson(personId)

        call.respond(status = HttpStatusCode.OK, message = TelleverkResponse(
            telleverk = tellerverk,
            maksdato = maksdato,
            sisteUtbetalingDato = sisteUtbetalingDato
        ))
    }
}
