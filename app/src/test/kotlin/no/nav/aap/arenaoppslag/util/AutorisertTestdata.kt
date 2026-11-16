package no.nav.aap.arenaoppslag.util

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.aap.arenaoppslag.modeller.PersonId
import no.nav.aap.arenaoppslag.service.PersonService
import no.nav.aap.arenaoppslag.tilgangsmaskin.AuthorisertPersonId
import no.nav.aap.arenaoppslag.tilgangsmaskin.FakeTilgangmaskinGateway
import no.nav.aap.arenaoppslag.tilgangsmaskin.PersonTilgangResultat
import no.nav.aap.arenaoppslag.tilgangsmaskin.TilgangkontrollService
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken

fun autorisertPerson(personId: PersonId): AuthorisertPersonId = runBlocking {
    val personService = mockk<PersonService>()
    every { personService.hentPersonId("testperson") } returns personId
    val service = TilgangkontrollService(FakeTilgangmaskinGateway(), mockk(), personService)
    val resultat = service.verifiserTilgangTilPerson("testperson", mockk<OidcToken>())
    check(resultat is PersonTilgangResultat.Granted)
    resultat.autorisertPerson
}
