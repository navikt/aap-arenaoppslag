package no.nav.aap.arenaoppslag.util

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.aap.arenaoppslag.modeller.ArenaPersonId
import no.nav.aap.arenaoppslag.modeller.PersonIdentifikator
import no.nav.aap.arenaoppslag.service.PersonService
import no.nav.aap.arenaoppslag.tilgangsmaskin.AuthorisertPersonId
import no.nav.aap.arenaoppslag.tilgangsmaskin.FakeTilgangmaskinGateway
import no.nav.aap.arenaoppslag.tilgangsmaskin.PersonTilgangResultat
import no.nav.aap.arenaoppslag.tilgangsmaskin.TilgangkontrollService
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken

fun autorisertPerson(personId: ArenaPersonId): AuthorisertPersonId = runBlocking {
    val personService = mockk<PersonService>()
    val testperson = PersonIdentifikator("12312312312")
    every { personService.hentArenaPersonidentifikator(testperson) } returns personId
    val service = TilgangkontrollService(FakeTilgangmaskinGateway(), mockk(), personService)
    val resultat = service.verifiserTilgangTilPerson(testperson, mockk<OidcToken>())
    check(resultat is PersonTilgangResultat.Granted)
    resultat.autorisertPerson
}
