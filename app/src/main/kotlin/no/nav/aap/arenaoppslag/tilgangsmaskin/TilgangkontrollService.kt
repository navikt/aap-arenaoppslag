package no.nav.aap.arenaoppslag.tilgangsmaskin

import no.nav.aap.arenaoppslag.modeller.PersonId
import no.nav.aap.arenaoppslag.modeller.Saksnummer
import no.nav.aap.arenaoppslag.service.PersonService
import no.nav.aap.arenaoppslag.service.SakService
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import org.slf4j.LoggerFactory

/**
 * Bevis på at kalleren er autorisert for tilgang til data om en bestemt person, og inneholder den oppslåtte
 * interne [PersonId]-en som brukes ved videre kall til tjenester og repositorier.
 * Private constructor slik at det ikke opprettes instanser uten at det er gjennomtenkt av utvikleren.
 */
class AuthorisertPersonId private constructor(val personId: PersonId) {
    companion object {
        fun createInstance(personId: PersonId): AuthorisertPersonId {
            return AuthorisertPersonId(personId)
        }
    }
}

/**
 * Bevis på at kalleren er autorisert for tilgang til en bestemt sak.
 * Private constructor slik at det ikke opprettes instanser uten at det er gjennomtenkt av utvikleren.
 */
data class AutorisertSaksnummer private constructor(val saksnummer: String) {
    companion object {
        fun createInstance(saksnummer: String): AutorisertSaksnummer {
            return AutorisertSaksnummer(saksnummer)
        }
    }

    fun toSaksnummer() = Saksnummer.fromString(saksnummer)!!
}

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
