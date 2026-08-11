package no.nav.aap.arenaoppslag.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.arenaoppslag.database.SakRepository
import no.nav.aap.arenaoppslag.database.VedtakfaktaRepository
import no.nav.aap.arenaoppslag.database.VilkårsvurderingRepository
import no.nav.aap.arenaoppslag.modeller.ArenaSakOppsummering
import no.nav.aap.arenaoppslag.modeller.ArenaVedtakfakta
import no.nav.aap.arenaoppslag.modeller.ArenaVilkårsvurdering
import no.nav.aap.arenaoppslag.modeller.Maksdatolinje
import no.nav.aap.arenaoppslag.modeller.PersonId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SakServiceTest {

    private val personId = PersonId(42)

    @Test
    fun `hentSakerForPerson mapper saker til kontrakt og cacher per person`() {
        val sakRepository = mockk<SakRepository>()
        val vedtakfaktaRepository = mockk<VedtakfaktaRepository>()
        val vilkårsvurderingRepository = vilkårsvurderingRepositoryMedAAARBEID1Oppfylt()
        every { sakRepository.hentSakerForPerson(personId) } returns listOf(
            arenaSakOppsummering(sakId = "1"),
            arenaSakOppsummering(sakId = "2"),
        )

        val service = SakService(sakRepository, vedtakfaktaRepository, vilkårsvurderingRepository)

        val foersteKall = service.hentSakerForPerson(personId)
        val andreKall = service.hentSakerForPerson(personId)

        assertThat(foersteKall.saker.map { it.sakId }).containsExactly("1", "2")
        assertThat(andreKall).isSameAs(foersteKall)
        verify(exactly = 1) { sakRepository.hentSakerForPerson(personId) }
    }

    @Test
    fun `hentMaksdatoAapMedVedtakOgSak mapper maksdatolinjer til kontrakt`() {
        val sakRepository = mockk<SakRepository>()
        val vedtakfaktaRepository = mockk<VedtakfaktaRepository>()
        val vilkårsvurderingRepository = vilkårsvurderingRepositoryMedAAARBEID1Oppfylt()

        val maxdato = LocalDate.of(2026, 5, 1)
        every { sakRepository.hentMaxdatoForSisteVedtak(personId) } returns
            maksdatolinje(sakId = 1, vedtaktypeKode = "O", sakStatus = "AKTIV", maxdato = maxdato)
        every { vedtakfaktaRepository.hentForVedtakIder(any()) } returns emptyMap()

        val resultat = SakService(sakRepository, vedtakfaktaRepository, vilkårsvurderingRepository).hentMaksdatoAapMedVedtakOgSak(personId)

        assertThat(resultat).isNotNull
        assertThat(resultat?.sakId).isEqualTo(1)
        assertThat(resultat?.erLopende()).isTrue()
        assertThat(resultat?.sisteVedtak?.maxdatoAap).isEqualTo(maxdato)
    }

    @Test
    fun `hentMaksdatoAapMedVedtakOgSak setter unntaksvilkaar fra vedtakfakta paa forventet format`() {
        val sakRepository = mockk<SakRepository>()
        val vedtakfaktaRepository = mockk<VedtakfaktaRepository>()
        val vilkårsvurderingRepository = vilkårsvurderingRepositoryMedAAARBEID1Oppfylt()

        val maxdato = LocalDate.of(2026, 5, 1)
        val unntaksdato = LocalDate.of(2025, 2, 1)
        every { sakRepository.hentMaxdatoForSisteVedtak(personId) } returns
            maksdatolinje(sakId = 1, vedtaktypeKode = "O", sakStatus = "AKTIV", maxdato = maxdato)
        // vedtakId i maksdatolinje-hjelperen er sakId * 10
        every { vedtakfaktaRepository.hentForVedtakIder(listOf(10)) } returns mapOf(
            10 to listOf(
                vedtakfakta(kode = "UNNTAKAAP", verdi = "J"),
                vedtakfakta(kode = "AAPVILKUNN", verdi = "01-02-2025"),
            )
        )

        val resultat = SakService(sakRepository, vedtakfaktaRepository, vilkårsvurderingRepository).hentMaksdatoAapMedVedtakOgSak(personId)

        assertThat(resultat).isNotNull
        assertThat(resultat?.unntaksvilkaarInnvilget).isTrue()
        assertThat(resultat?.unntaksvilkaarGjelderFra).isEqualTo(unntaksdato)
    }

    @Test
    fun `hentMaksdatoAapMedVedtakOgSak bruker vilkårsvurdering for ukjent UNNTAKAAP-verdi`() {
        val sakRepository = mockk<SakRepository>()
        val vedtakfaktaRepository = mockk<VedtakfaktaRepository>()
        val vilkårsvurderingRepository = vilkårsvurderingRepositoryMedAAARBEID1Oppfylt()

        every { sakRepository.hentMaxdatoForSisteVedtak(personId) } returns
            maksdatolinje(sakId = 1, vedtaktypeKode = "O", sakStatus = "AKTIV", maxdato = LocalDate.of(2026, 5, 1))
        every { vedtakfaktaRepository.hentForVedtakIder(listOf(10)) } returns mapOf(
            10 to listOf(vedtakfakta(kode = "UNNTAKAAP", verdi = "APPELSIN"))
        )

        val resultat = SakService(sakRepository, vedtakfaktaRepository, vilkårsvurderingRepository).hentMaksdatoAapMedVedtakOgSak(personId)

        assertThat(resultat?.unntaksvilkaarInnvilget).isTrue()
    }

    @Test
    fun `hentMaksdatoAapMedVedtakOgSak bruker vilkårsvurdering naar vedtakfakta mangler`() {
        val sakRepository = mockk<SakRepository>()
        val vedtakfaktaRepository = mockk<VedtakfaktaRepository>()
        val vilkårsvurderingRepository = vilkårsvurderingRepositoryMedAAARBEID1Oppfylt()

        every { sakRepository.hentMaxdatoForSisteVedtak(personId) } returns
            maksdatolinje(sakId = 1, vedtaktypeKode = "O", sakStatus = "AKTIV", maxdato = LocalDate.of(2026, 5, 1))
        every { vedtakfaktaRepository.hentForVedtakIder(listOf(10)) } returns emptyMap()

        val resultat = SakService(sakRepository, vedtakfaktaRepository, vilkårsvurderingRepository).hentMaksdatoAapMedVedtakOgSak(personId)

        assertThat(resultat).isNotNull
        assertThat(resultat?.unntaksvilkaarInnvilget).isTrue()
        assertThat(resultat?.unntaksvilkaarGjelderFra).isNull()
    }

    @Test
    fun `hentMaksdatoAapMedVedtakOgSak bruker vilkårsvurdering naar relevante koder mangler`() {
        val sakRepository = mockk<SakRepository>()
        val vedtakfaktaRepository = mockk<VedtakfaktaRepository>()
        val vilkårsvurderingRepository = vilkårsvurderingRepositoryMedAAARBEID1Oppfylt()

        every { sakRepository.hentMaxdatoForSisteVedtak(personId) } returns
            maksdatolinje(sakId = 1, vedtaktypeKode = "O", sakStatus = "AKTIV", maxdato = LocalDate.of(2026, 5, 1))
        every { vedtakfaktaRepository.hentForVedtakIder(listOf(10)) } returns mapOf(
            10 to listOf(vedtakfakta(kode = "NOEANNET", verdi = "J"))
        )

        val resultat = SakService(sakRepository, vedtakfaktaRepository, vilkårsvurderingRepository).hentMaksdatoAapMedVedtakOgSak(personId)

        assertThat(resultat?.unntaksvilkaarInnvilget).isTrue()
        assertThat(resultat?.unntaksvilkaarGjelderFra).isNull()
    }

    @Test
    fun `hentMaksdatoAapForPerson returnerer maksdato for lopende vedtak`() {
        val sakRepository = mockk<SakRepository>()
        val vedtakfaktaRepository = mockk<VedtakfaktaRepository>()
        val vilkårsvurderingRepository = vilkårsvurderingRepositoryMedAAARBEID1Oppfylt()

        val senest = LocalDate.of(2027, 6, 30)
        every { sakRepository.hentMaxdatoForSisteVedtak(personId) } returns
            maksdatolinje(sakId = 3, vedtaktypeKode = "O", sakStatus = "AKTIV", maxdato = senest)
        every { vedtakfaktaRepository.hentForVedtakIder(any()) } returns emptyMap()

        val resultat = SakService(sakRepository, vedtakfaktaRepository, vilkårsvurderingRepository).hentMaksdatoAapForPerson(personId)

        assertThat(resultat).isEqualTo(senest)
    }


    @Test
    fun `hentMaksdatoAapForPerson returnerer null naar lopende vedtak mangler maksdato`() {
        val sakRepository = mockk<SakRepository>()
        val vedtakfaktaRepository = mockk<VedtakfaktaRepository>()
        val vilkårsvurderingRepository = vilkårsvurderingRepositoryMedAAARBEID1Oppfylt()

        every { sakRepository.hentMaxdatoForSisteVedtak(personId) } returns
            maksdatolinje(sakId = 1, vedtaktypeKode = "O", sakStatus = "AKTIV", maxdato = null)
        every { vedtakfaktaRepository.hentForVedtakIder(any()) } returns emptyMap()

        val resultat = SakService(sakRepository, vedtakfaktaRepository, vilkårsvurderingRepository).hentMaksdatoAapForPerson(personId)

        assertThat(resultat).isNull()
    }

    @Test
    fun `hentMaksdatoAapForPerson returnerer null naar det ikke finnes noen saker`() {
        val sakRepository = mockk<SakRepository>()
        val vedtakfaktaRepository = mockk<VedtakfaktaRepository>()
        val vilkårsvurderingRepository = vilkårsvurderingRepositoryMedAAARBEID1Oppfylt()

        every { sakRepository.hentMaxdatoForSisteVedtak(personId) } returns null

        val resultat = SakService(sakRepository, vedtakfaktaRepository, vilkårsvurderingRepository).hentMaksdatoAapForPerson(personId)

        assertThat(resultat).isNull()
    }

    private fun arenaSakOppsummering(sakId: String) = ArenaSakOppsummering(
        sakId = sakId,
        lopenummer = 1,
        aar = 2026,
        antallVedtak = 1,
        sakstype = "AAP",
        statuskode = "AKTIV",
        statusnavn = "Aktiv",
        regDato = LocalDate.of(2026, 1, 1),
        avsluttetDato = null,
    )

    private fun vedtakfakta(kode: String, verdi: String?) = ArenaVedtakfakta(
        kode = kode,
        navn = kode,
        verdi = verdi,
        registrertDato = LocalDate.of(2025, 1, 1),
    )

    private fun vilkårsvurderingRepositoryMedAAARBEID1Oppfylt(): VilkårsvurderingRepository {
        val repo = mockk<VilkårsvurderingRepository>()
        every { repo.hentForVedtakIder(any()) } answers {
            @Suppress("UNCHECKED_CAST")
            val vedtakIder = args[0] as List<Int>
            vedtakIder.associateWith {
                listOf(vilkårsvurdering(vilkårkode = "AAARBEID1", statuskode = "J"))
            }
        }
        return repo
    }

    private fun vilkårsvurdering(vilkårkode: String, statuskode: String) = ArenaVilkårsvurdering(
        vilkårsvurderingId = 1L,
        vilkårkode = vilkårkode,
        begrunnelse = null,
        vurdertAv = null,
        vilkårnavn = vilkårkode,
        erObligatorisk = true,
        hjelpetekstUrl = null,
        lovtekstUrl = null,
        rundskrivUrl = null,
        statuskode = statuskode,
        statusnavn = statuskode, // forenkling, samme som kode her i testen
    )

    private fun maksdatolinje(
        sakId: Int,
        vedtaktypeKode: String,
        sakStatus: String,
        maxdato: LocalDate?,
    ) = Maksdatolinje(
        sakId = sakId,
        opprettetAar = 2025,
        lopenr = 12,
        vedtakId = sakId * 10,
        aktfaseKode = "FA",
        vedtaktypeKode = vedtaktypeKode,
        fra = LocalDate.of(2025, 1, 1),
        til = LocalDate.of(2026, 1, 1),
        maxdatoUnntak = null,
        maxdatoOrdinaer = maxdato,
        sakRegistrert = LocalDate.of(2025, 1, 1),
        sakAvsluttet = null,
        sakStatus = sakStatus,
    )
}
