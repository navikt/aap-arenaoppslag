package no.nav.aap.arenaoppslag.tilgangsmaskin

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.aap.arenaoppslag.modeller.ArenaPersonId
import no.nav.aap.arenaoppslag.modeller.PersonIdentifikator
import no.nav.aap.arenaoppslag.modeller.Saksnummer
import no.nav.aap.arenaoppslag.service.PersonService
import no.nav.aap.arenaoppslag.service.SakService
import no.nav.aap.arenaoppslag.util.AzureTokenGen
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TilgangkontrollServiceTest {
    private val gateway = mockk<TilgangsmaskinGateway>()
    private val personService = mockk<PersonService>()
    private val sakService = mockk<SakService>()
    private val service = TilgangkontrollService(gateway, sakService, personService)
    private val token = OidcToken(AzureTokenGen("issuer", "arenaoppslag", "Z123456").generate())
    private val saksnummer = Saksnummer(1, 2021)
    private val testperson = PersonIdentifikator("12312312312")

    @Test
    fun `personbevis opprettes etter godkjent tilgang og personoppslag`(): Unit = runBlocking {
        coEvery { gateway.harTilgangTilPerson(testperson, token) } returns TilgangsmaskinKomplettResponse(true)
        every { personService.hentArenaPersonId(testperson) } returns ArenaPersonId(42)
        val resultat = service.verifiserTilgangTilPerson(testperson, token)
        check(resultat is PersonTilgangResultat.Granted)
        assertThat(resultat.autorisertPerson.personId).isEqualTo(ArenaPersonId(42))
    }

    @Test
    fun `avvist person blir ikke slaatt opp`(): Unit = runBlocking {
        coEvery { gateway.harTilgangTilPerson(testperson, token) } returns TilgangsmaskinKomplettResponse(false)
        assertThat(service.verifiserTilgangTilPerson(testperson, token))
            .isInstanceOf(PersonTilgangResultat.AccessDenied::class.java)
        verify(exactly = 0) { personService.hentArenaPersonId(any<PersonIdentifikator>()) }
    }

    @Test
    fun `godkjent tilgang til ukjent person gir ikke bevis`(): Unit = runBlocking {
        coEvery { gateway.harTilgangTilPerson(testperson, token) } returns TilgangsmaskinKomplettResponse(true)
        every { personService.hentArenaPersonId(testperson) } returns null
        assertThat(service.verifiserTilgangTilPerson(testperson, token)).isEqualTo(PersonTilgangResultat.NotFound)
    }

    @Test
    fun `sakbevis gjelder saken som ble kontrollert`(): Unit = runBlocking {
        every { personService.hentArenaPersonId(saksnummer) } returns testperson
        coEvery { gateway.harTilgangTilPerson(testperson, token) } returns TilgangsmaskinKomplettResponse(true)
        val resultat = service.verifiserTilgangTilSak(saksnummer, token)
        check(resultat is SakTilgangResultat.Granted)
        assertThat(resultat.autorisertSaksnummer.saksnummer).isEqualTo(saksnummer)
    }

    @Test
    fun `avvist sak gir ikke bevis`(): Unit = runBlocking {
        every { personService.hentArenaPersonId(saksnummer) } returns testperson
        coEvery { gateway.harTilgangTilPerson(testperson, token) } returns TilgangsmaskinKomplettResponse(false)
        assertThat(service.verifiserTilgangTilSak(saksnummer, token))
            .isInstanceOf(SakTilgangResultat.AccessDenied::class.java)
    }

    @Test
    fun `ukjent sak gir ikke bevis`(): Unit = runBlocking {
        every { personService.hentArenaPersonId(saksnummer) } returns null
        assertThat(service.verifiserTilgangTilSak(saksnummer, token)).isEqualTo(SakTilgangResultat.NotFound)
        coVerify(exactly = 0) { gateway.harTilgangTilPerson(any(), any()) }
    }

    @Test
    fun `feil fra tilgangsmaskinen gir ikke bevis`(): Unit = runBlocking {
        coEvery { gateway.harTilgangTilPerson(testperson, token) } throws IllegalStateException("utilgjengelig")
        assertThrows<IllegalStateException> { service.verifiserTilgangTilPerson(testperson, token) }
        verify(exactly = 0) { personService.hentArenaPersonId(any<PersonIdentifikator>()) }
    }
}
