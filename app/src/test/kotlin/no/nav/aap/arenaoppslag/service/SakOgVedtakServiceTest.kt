package no.nav.aap.arenaoppslag.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.arenaoppslag.SakOgVedtakService
import no.nav.aap.arenaoppslag.database.SakRepository
import no.nav.aap.arenaoppslag.database.VedtakRepository
import no.nav.aap.arenaoppslag.database.VedtakfaktaRepository
import no.nav.aap.arenaoppslag.database.VilkårsvurderingRepository
import no.nav.aap.arenaoppslag.modeller.ArenaSak
import no.nav.aap.arenaoppslag.modeller.ArenaSakPerson
import no.nav.aap.arenaoppslag.modeller.ArenaVedtak
import no.nav.aap.arenaoppslag.modeller.ArenaVedtakRad
import no.nav.aap.arenaoppslag.modeller.ArenaPersonId
import no.nav.aap.arenaoppslag.modeller.SakId
import no.nav.aap.arenaoppslag.util.autorisertPerson
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class SakOgVedtakServiceTest {

    private val sakRepository = mockk<SakRepository>()
    private val vedtakRepository = mockk<VedtakRepository>()
    private val vedtakfaktaRepository = mockk<VedtakfaktaRepository>()
    private val vilkårsvurderingRepository = mockk<VilkårsvurderingRepository>()

    private val service = SakOgVedtakService(
        sakRepository, vedtakRepository, vedtakfaktaRepository, vilkårsvurderingRepository
    )

    @Test
    fun `hentVedtakForPerson bruker person-id fra autorisert person og returnerer vedtakene`() {
        val personId = ArenaPersonId(42)
        val autorisert = autorisertPerson(personId)
        val førsteVedtak = ArenaVedtak(
            vedtakId = 101,
            sakId = "1",
            statusKode = "IVERK",
            vedtaktypeKode = "O",
            fraOgMed = LocalDate.of(2024, 1, 1),
            tilDato = LocalDate.of(2025, 1, 1),
            rettighetkode = "AAP",
            utfallkode = "JA",
            aktivitetsfaseKode = "IKKE",
        )
        val andreVedtak = førsteVedtak.copy(vedtakId = 201, sakId = "2")
        every { vedtakRepository.hentVedtak(autorisert) } returns listOf(førsteVedtak, andreVedtak)

        val vedtak = service.hentVedtakForPerson(autorisert)

        assertThat(vedtak).containsExactly(førsteVedtak, andreVedtak)
        verify(exactly = 1) { vedtakRepository.hentVedtak(autorisert) }
    }

    @Test
    fun `hentVedtakForPerson returnerer tom liste når autorisert person ikke har vedtak`() {
        val personId = ArenaPersonId(99)
        val autorisert = autorisertPerson(personId)
        every { vedtakRepository.hentVedtak(autorisert) } returns emptyList()

        val vedtak = service.hentVedtakForPerson(autorisert)

        assertThat(vedtak).isEmpty()
        verify(exactly = 1) { vedtakRepository.hentVedtak(autorisert) }
    }

    @Test
    fun `hentVedtakDetaljerForPerson returnerer flattened vedtakliste fra alle saker`() {
        val personId = ArenaPersonId(42)
        val autorisert = autorisertPerson(personId)
        val sak1 = lagArenaSak(sakId = "1")
        val sak2 = lagArenaSak(sakId = "2")

        every { sakRepository.hentSakerDetaljerForPerson(autorisert) } returns listOf(sak1, sak2)
        every { vedtakRepository.hentVedtakForSak(SakId(1)) } returns listOf(lagVedtakRad(101), lagVedtakRad(102))
        every { vedtakRepository.hentVedtakForSak(SakId(2)) } returns listOf(lagVedtakRad(201))
        every { vedtakfaktaRepository.hentForVedtakIder(listOf(101, 102)) } returns emptyMap()
        every { vedtakfaktaRepository.hentForVedtakIder(listOf(201)) } returns emptyMap()
        every { vilkårsvurderingRepository.hentForVedtakIder(listOf(101, 102)) } returns emptyMap()
        every { vilkårsvurderingRepository.hentForVedtakIder(listOf(201)) } returns emptyMap()

        val vedtak = service.hentVedtakDetaljerForPerson(autorisert)

        assertThat(vedtak).hasSize(3)
        assertThat(vedtak.map { it.vedtakId }).containsExactlyInAnyOrder(101, 102, 201)
    }

    @Test
    fun `hentVedtakDetaljerForPerson returnerer tom liste når person ikke har saker`() {
        val personId = ArenaPersonId(99)
        val autorisert = autorisertPerson(personId)
        every { sakRepository.hentSakerDetaljerForPerson(autorisert) } returns emptyList()

        val vedtak = service.hentVedtakDetaljerForPerson(autorisert)

        assertThat(vedtak).isEmpty()
    }

    private fun lagArenaSak(sakId: String) = ArenaSak(
        sakId = sakId,
        opprettetAar = 2024,
        lopenr = sakId.toInt(),
        statuskode = "AKTIV",
        statusnavn = "Aktiv",
        registrertDato = LocalDateTime.of(2024, 1, 1, 0, 0),
        avsluttetDato = null,
        person = ArenaSakPerson(
            personId = 42,
            fodselsnummer = "12312312312",
            fornavn = "Test",
            etternavn = "Testesen",
        )
    )

    private fun lagVedtakRad(vedtakId: Int) = ArenaVedtakRad(
        vedtakId = vedtakId,
        lopenrvedtak = vedtakId,
        statusKode = "IVERK",
        statusNavn = "Iverksatt",
        vedtaktypeKode = "O",
        vedtaktypeNavn = "Ny rettighet",
        aktivitetsfaseKode = "IKKE",
        aktivitetsfaseNavn = "Ikke fastsatt",
        fraOgMed = LocalDate.of(2024, 1, 1),
        tilDato = LocalDate.of(2025, 1, 1),
        rettighetkode = "AAP",
        rettighetnavn = "Arbeidsavklaringspenger",
        utfallkode = "JA",
        begrunnelse = null,
        saksbehandler = null,
        beslutter = null,
        relatertVedtak = null,
    )
}
