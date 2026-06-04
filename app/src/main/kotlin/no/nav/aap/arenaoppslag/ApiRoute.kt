package no.nav.aap.arenaoppslag

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.arenaoppslag.kontrakt.apiv1.ArenaVedtakMedDetaljer
import no.nav.aap.arenaoppslag.kontrakt.apiv1.MaksdatoRequest
import no.nav.aap.arenaoppslag.kontrakt.apiv1.MaksdatoResponse
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SakerResponse
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SisteUtbetalingerRequest
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SisteUtbetalingerResponse
import no.nav.aap.arenaoppslag.kontrakt.intern.PersonEksistererIAAPArena
import no.nav.aap.arenaoppslag.kontrakt.intern.SakerRequest
import no.nav.aap.arenaoppslag.kontrakt.intern.SignifikanteSakerRequest
import no.nav.aap.arenaoppslag.kontrakt.intern.SignifikanteSakerResponse
import no.nav.aap.arenaoppslag.kontrakt.intern.TellerRequest
import no.nav.aap.arenaoppslag.modeller.ArenaSakMedVedtak
import no.nav.aap.arenaoppslag.kontrakt.apiv1.VedtakForPersonRequest
import no.nav.aap.arenaoppslag.modeller.PersonId
import no.nav.aap.arenaoppslag.modeller.SakIdentifikator
import no.nav.aap.arenaoppslag.service.HistorikkService
import no.nav.aap.arenaoppslag.service.PersonService
import no.nav.aap.arenaoppslag.service.PosteringService
import no.nav.aap.arenaoppslag.service.SakOgVedtakService
import no.nav.aap.arenaoppslag.service.SakService
import no.nav.aap.arenaoppslag.service.TelleverkService
import no.nav.aap.arenaoppslag.service.TilgangService
import no.nav.aap.arenaoppslag.util.token
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SakerRequest as SakerRequestV1

fun Route.historikk(historikkService: HistorikkService) {
    post("/person/signifikant-historikk") {
        logger.info("Sjekker om personens AAP-Arena-historikk er signifikant for saksbehandling i Kelvin")
        val request: SignifikanteSakerRequest = call.receive()
        val response: SignifikanteSakerResponse = historikkService.signifikanteSakerForPerson(
            request.personidentifikatorer.toSet(), request.virkningstidspunkt
        )

        call.respond(response)
    }

    post("/person/eksisterer") {
        logger.info("Sjekker om person eksisterer i AAP-Arena")
        val request: SakerRequest = call.receive()
        val response: PersonEksistererIAAPArena =
            historikkService.personEksistererIAapArena(request.personidentifikatorer.toSet())

        call.respond(response)
    }
}

fun Route.sakerForPerson(sakService: SakService, personService: PersonService) {
    post("/person/saker") {
        logger.info("Henter saker for person")
        val request: SakerRequestV1 = call.receive()

        val personidentifikator = request.personidentifikator
        val personId = personService.hentPersonId(personidentifikator)
            ?: return@post call.respond(HttpStatusCode.NotFound, "Fant ikke personen i Arena")

        val respons: SakerResponse = sakService.hentSakerForPerson(personId)

        call.respond(HttpStatusCode.OK, respons)
    }
}

fun Route.maksdato(sakService: SakService, personService: PersonService) {
    post("/maksdato") {
        logger.info("Henter maksdato-AAP for saksliste")
        val request: MaksdatoRequest = call.receive()
        val personidentifikator = request.personidentifikator
        val personId = personService.hentPersonId(personidentifikator)
            ?: return@post call.respond(HttpStatusCode.NotFound, "Fant ikke personen i Arena")

        val saker = sakService.hentMaksdatoAapForVedtakISaker(personId)

        // dersom personen finnes i Arena men ikke har AAP-vedtak utenfor Stans blir listen tom
        call.respond(HttpStatusCode.OK, MaksdatoResponse(saker))
    }
}

fun Route.sak(sakService: SakService, posteringService: PosteringService, sakOgVedtakService: SakOgVedtakService, telleverkService: TelleverkService, tilgangService: TilgangService) {
    get("/sak/{sakid}/detaljert") {
        val sakid = call.parameters["sakid"]

        if (sakid == null) {
            logger.info("Sakid kan ikke være NULL")
            return@get call.respond(HttpStatusCode.BadRequest)
        }

        val sakIdentifikator = SakIdentifikator.fromString(sakid)

        if (sakIdentifikator == null) {
            logger.info("Klarte ikke tolke saksnummer: $sakid")
            return@get call.respond(HttpStatusCode.BadRequest)
        }

        tilgangService.harTilgangTilSak(sakIdentifikator, call.token())
            .mapNotNull { sakOgVedtakService.hentSakMedVedtak(sakIdentifikator) }
            .map { sak: ArenaSakMedVedtak ->
                val personId = PersonId(sak.person.personId)

                logger.info("Henter saksdetaljer")
                val kvoteHistorikk = telleverkService.hentKvoteBrukHendelserForPerson(personId)
                val telleverk = telleverkService.hentTelleverkForPerson(personId)
                val maksdato = sakService.hentMaksdatoAapForPerson(personId)
                val sisteUtbetalingDato = posteringService.hentSisteAapUtbetalingForPerson(personId)

                sak.tilKontrakt( telleverk,kvoteHistorikk,sisteUtbetalingDato,maksdato)
            }.fold(
                suksess = { call.respond(status = HttpStatusCode.OK, message = it) },
                ikkeFunnet = {
                    logger.info("Klarte ikke hente sak for saksnummer $sakid")
                    call.respond(HttpStatusCode.NotFound)
                },
                ikkeTilgang = {
                    logger.info("Bruker har ikke tilgang til å behandle sak $sakid")
                    call.respond(HttpStatusCode.Forbidden)
                }
            )
    }
}

fun Route.vedtakDetaljerForPerson(sakOgVedtakService: SakOgVedtakService, personService: PersonService) {
    post("/person/vedtak") {
        logger.info("Henter alle vedtak for person")
        val request: VedtakForPersonRequest = call.receive()

        val personId = personService.hentPersonId(request.personidentifikator)
            ?: return@post call.respond(HttpStatusCode.NotFound, "Fant ikke personen i Arena")

        val vedtak: List<ArenaVedtakMedDetaljer> = sakOgVedtakService.hentVedtakDetaljerForPerson(personId)
            .map { it.tilKontrakt() }

        call.respond(HttpStatusCode.OK, vedtak)
    }
}

fun Route.telleverk(telleverkService: TelleverkService, personService: PersonService, tilgangService: TilgangService) {
    post("/telleverk") {
        logger.info("Henter telleverk")
        val request: TellerRequest = call.receive()
        val personidentifikator = request.personidentifikator

        tilgangService.harTilgangTilPerson(personidentifikator, call.token())
            .mapNotNull { personService.hentPersonId(personidentifikator) }
            .onError { logger.info("Fant ikke person i Arena for henting av telleverk") }
            .mapNotNull { telleverkService.hentTelleverkForPerson(it) }
            .onError { logger.info("Fant ikke telleverk for person i Arena") }
            .fold(
                suksess = { call.respond(status = HttpStatusCode.OK, message = it) },
                ikkeFunnet = { call.respond(HttpStatusCode.NotFound, "Fant ikke telleverk for person") },
                ikkeTilgang = { call.respond(HttpStatusCode.Forbidden) }
            )
    }
}

fun Route.utbetalinger(posteringService: PosteringService, personService: PersonService) {
    post("/utbetalinger/siste") {
        logger.info("Henter maksdato-AAP for saksliste")
        val request: SisteUtbetalingerRequest = call.receive()

        val personidentifikator = request.personidentifikator
        val personId = personService.hentPersonId(personidentifikator)
            ?: return@post call.respond(HttpStatusCode.NotFound, "Fant ikke personen i Arena")

        val utbetaling = posteringService.hentSisteAapUtbetalingForPerson(personId)

        // returnerer tom liste dersom personen ikke har AAP-utbetalinger i Arena
        call.respond(HttpStatusCode.OK, SisteUtbetalingerResponse(utbetaling))
    }
}
