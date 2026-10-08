package no.nav.aap.arenaoppslag.tilgangsmaskin

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.arenaoppslag.modeller.Saksnummer
import no.nav.aap.arenaoppslag.service.PersonService
import no.nav.aap.arenaoppslag.service.SakService
import no.nav.aap.arenaoppslag.util.token
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import org.slf4j.LoggerFactory

class TilgangkontrollService(
    private val tilgangmaskinGateway: TilgangmaskinGateway,
    private val sakService: SakService,
    private val personService: PersonService,
) {
    companion object {
        private val logger = LoggerFactory.getLogger("TilgangkontrollService")
    }

    /**
     * Tilgang først: Hvis tokenet ikke gir lesetilgang til denne identifikatoren, returneres
     * `AccessDenied` uten å avsløre om personen finnes. Hvis tilgang gis, slås person-ID-en opp,
     * og `NotFound` returneres bare til kallere som har lov til å vite at personen mangler.
     */
    fun verifiserTilgangTilPerson(personIdentifikator: String, token: OidcToken): PersonTilgangResultat {
        if (!tilgangmaskinGateway.harTilgangTilPerson(personIdentifikator, token)) {
            logger.info("Ikke tilgang til angitt person for navIdent=${token.navIdent()}")
            return PersonTilgangResultat.AccessDenied
        }

        val personId = personService.hentPersonId(personIdentifikator)
        if (personId == null) {
            logger.info("Fant ikke person med angitt personIdentifikator i AAP-Arena")
            return PersonTilgangResultat.NotFound
        }

        return PersonTilgangResultat.Granted(AuthorisertPersonId.createInstance(personId))
    }

    suspend fun medVerifisertPersonTilgang(
        routingContext: RoutingContext,
        personidentifikator: String,
        onAccessDenied: suspend RoutingContext.() -> Unit = { call.respond(HttpStatusCode.Forbidden) },
        onNotFound: suspend RoutingContext.() -> Unit = {
            call.respond(HttpStatusCode.NotFound, "Fant ikke personen i Arena")
        },
        onGranted: suspend RoutingContext.(PersonTilgangResultat.Granted) -> Unit,
    ) {
        val tilgang = verifiserTilgangTilPerson(personidentifikator, routingContext.call.token())
        routingContext.medTilgangKontrollert(tilgang, onAccessDenied, onNotFound, onGranted)
    }

    /**
     * Finner personen bak [saksnummer] og kontrollerer deretter om tokenet gir tilgang til personen.
     * Returnerer et sealed result slik at kallere kan skille mellom `NotFound` og `AccessDenied`.
     * Vi velger å opplyse om saken finnes eller ikke til alle kallere.
     */
    fun verifiserTilgangTilSak(saksnummer: Saksnummer, token: OidcToken): SakTilgangResultat {
        val person = sakService.hentPersonForSak(saksnummer)
        if (person == null) {
            logger.info("Fant ikke saksnummer=$saksnummer i AAP-Arena med en person tilknyttet")
            return SakTilgangResultat.NotFound
        }

        if (!tilgangmaskinGateway.harTilgangTilPerson(person.fodselsnummer, token)) {
            logger.info("Ikke tilgang til saksnummer=$saksnummer for navIdent=${token.navIdent()}")
            return SakTilgangResultat.AccessDenied
        }
        return SakTilgangResultat.Granted(AutorisertSaksnummer.createInstance(saksnummer.toString()))
    }

    suspend fun medVerifisertSakTilgang(
        routingContext: RoutingContext,
        saksnummer: Saksnummer,
        onAccessDenied: suspend RoutingContext.() -> Unit = { call.respond(HttpStatusCode.Forbidden) },
        onNotFound: suspend RoutingContext.() -> Unit = {
            call.respond(HttpStatusCode.NotFound, "Fant ikke saken i Arena")
        },
        onGranted: suspend RoutingContext.(SakTilgangResultat.Granted) -> Unit,
    ) {
        val tilgang = verifiserTilgangTilSak(saksnummer, routingContext.call.token())
        routingContext.medTilgangKontrollert(tilgang, onAccessDenied, onNotFound, onGranted)
    }

}

sealed interface PersonTilgangResultat {
    data class Granted(val autorisertPerson: AuthorisertPersonId) : PersonTilgangResultat
    data object NotFound : PersonTilgangResultat
    data object AccessDenied : PersonTilgangResultat
}

sealed interface SakTilgangResultat {
    data class Granted(val autorisertSaksnummer: AutorisertSaksnummer) : SakTilgangResultat
    data object NotFound : SakTilgangResultat
    data object AccessDenied : SakTilgangResultat
}
