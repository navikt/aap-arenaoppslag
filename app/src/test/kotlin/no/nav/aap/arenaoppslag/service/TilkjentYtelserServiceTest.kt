package no.nav.aap.arenaoppslag.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.arenaoppslag.database.MeldekortRepository
import no.nav.aap.arenaoppslag.modeller.KvotebrukHendelse
import no.nav.aap.arenaoppslag.modeller.Meldekort
import no.nav.aap.arenaoppslag.modeller.MeldekortAnmerkning
import no.nav.aap.arenaoppslag.modeller.MeldekortDag
import no.nav.aap.arenaoppslag.modeller.MeldekortForSak
import no.nav.aap.arenaoppslag.modeller.MeldekortPostering
import no.nav.aap.arenaoppslag.modeller.MeldekortReduksjon
import no.nav.aap.arenaoppslag.modeller.Periode
import no.nav.aap.arenaoppslag.modeller.PersonId
import no.nav.aap.arenaoppslag.modeller.PosteringKilde
import no.nav.aap.arenaoppslag.modeller.SakId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TilkjentYtelserServiceTest {

    private val meldekortRepository = mockk<MeldekortRepository>()
    private val telleverkService = mockk<TelleverkService>()

    private val service = TilkjentYtelserService(meldekortRepository, telleverkService)

    @Test
    fun `komponerer rader med kilde, uke, dagsats og gjenstaaende kvoter`() {
        val sakId = SakId(9001)
        val periode = Periode(LocalDate.of(2023, 1, 2), LocalDate.of(2023, 1, 15))
        val meldekort = Meldekort(
            meldekortId = 5001,
            personId = 100,
            periode = periode,
            ukenrUke1 = 10,
            ukenrUke2 = 11,
            meldedato = null,
            meldeform = "E1",
            fortsattRegistrertArbeidssoker = true,
            kommentar = null,
            dager = listOf(MeldekortDag(10, 1, LocalDate.of(2023, 1, 2), 7.5, false)),
            reduksjon = MeldekortReduksjon(dagerForSent = 0, fravar = 0.0f, sykedager = 0.0f),
            anmerkninger = listOf(
                MeldekortAnmerkning(
                    kode = "FSNN",
                    navn = "Fravær av type S",
                    beskrivelse = "Utbetalingen er redusert pga sykdom &1 dager",
                    verdi = 3,
                    verdi2 = null,
                ),
            ),
        )

        every { meldekortRepository.hentForSak(sakId) } returns MeldekortForSak(
            posteringer = listOf(
                MeldekortPostering(
                    vedtakId = 90010, personId = 100, meldekortId = 5001, periode = periode,
                    belop = 4970, dagsatsMedBarnetillegg = 1812, dagsats = 1500, dagsatsForSamordning = 1500,
                    insGrad = null, kilde = PosteringKilde.MELDEKORT, kildeAlias = "MKORT", kildeObjektId = 5001,
                ),
                MeldekortPostering(
                    vedtakId = 90010, personId = 100, meldekortId = null, periode = periode,
                    belop = 3459, dagsatsMedBarnetillegg = null, dagsats = null, dagsatsForSamordning = null,
                    insGrad = null, kilde = PosteringKilde.SPESIALUTBETALING, kildeAlias = "SPESUTB",
                    kildeObjektId = 7700004,
                ),
            ),
            meldekort = listOf(meldekort),
        )
        every { telleverkService.hentKvoteBrukHendelserForPerson(PersonId(100)) } returns setOf(
            kvotebrukHendelse(id = 1, kvoteTypeKode = "AAP", grunnlag = "VEDTAK", objektId = 90010, resterende = 20),
            kvotebrukHendelse(id = 2, kvoteTypeKode = "MAAPU", grunnlag = "VEDTAK", objektId = 90010, resterende = 30),
            kvotebrukHendelse(id = 3, kvoteTypeKode = "AAP", grunnlag = "MKORT", objektId = 5001, resterende = 10),
        )

        val response = service.hentTilkjenteYtelserForSak(sakId)

        assertThat(response.sakId).isEqualTo(9001)
        assertThat(response.rader).hasSize(2)

        val meldekortRad = response.rader.first { it.kilde == PosteringKilde.MELDEKORT }
        assertThat(meldekortRad.uke).isEqualTo("10-11")
        assertThat(meldekortRad.dagsatsMedBarnetillegg).isEqualTo(1812)
        assertThat(meldekortRad.dagsats).isEqualTo(1500)
        assertThat(meldekortRad.beregnetBrutto).isEqualTo(4970)
        assertThat(meldekortRad.timerArbeidet).isEqualTo(7.5)
        // 7,5 timer av 10 arbeidsdager à 7,5 timer = 7,5 / 75 = 10 %. Ingen samordning (DAGS == DAGSFSAM).
        assertThat(meldekortRad.reduksjon?.levertForSentDager).isEqualTo(0)
        assertThat(meldekortRad.reduksjon?.timerArbeidetProsent).isEqualTo(10)
        assertThat(meldekortRad.reduksjon?.samordningsProsent).isEqualTo(0)
        assertThat(meldekortRad.reduksjon?.totalReduksjonProsent).isEqualTo(10)
        assertThat(meldekortRad.reduksjon?.fravar).isEqualTo(0.0f)
        assertThat(meldekortRad.reduksjon?.sykedager).isEqualTo(0.0f)
        assertThat(meldekortRad.reduksjon?.institusjonsProsent).isNull()
        assertThat(meldekortRad.meldekort?.uker).hasSize(1)
        assertThat(meldekortRad.meldekort?.fortsattRegistrertArbeidssoker).isTrue()
        // Anmerkningene følger meldekortet, og beskrivelsen flettes med verdien fra anmerkningen.
        assertThat(meldekortRad.meldekort?.anmerkninger).hasSize(1)
        val anmerkning = meldekortRad.meldekort?.anmerkninger?.single()
        assertThat(anmerkning?.kode).isEqualTo("FSNN")
        assertThat(anmerkning?.verdi).isEqualTo(3)
        assertThat(anmerkning?.beskrivelseFlettet).isEqualTo("Utbetalingen er redusert pga sykdom 3 dager")
        // Meldekortet trekker kun ordinær kvote, så unntakskvoten videreføres fra forrige bevegelse.
        assertThat(meldekortRad.gjenstaaendeOrdinaerDager).isEqualTo(10)
        assertThat(meldekortRad.gjenstaaendeUnntakDager).isEqualTo(30)

        val spesialRad = response.rader.first { it.kilde == PosteringKilde.SPESIALUTBETALING }
        assertThat(spesialRad.uke).isNull()
        assertThat(spesialRad.meldekort).isNull()
        assertThat(spesialRad.beregnetBrutto).isEqualTo(3459)
        assertThat(spesialRad.gjenstaaendeOrdinaerDager).isNull()
        assertThat(spesialRad.gjenstaaendeUnntakDager).isNull()
    }

    @Test
    fun `dager levert for sent trekkes fra beregningsgrunnlaget for timer arbeidet`() {
        val sakId = SakId(9001)
        val periode = Periode(LocalDate.of(2023, 1, 2), LocalDate.of(2023, 1, 15))
        val meldekort = Meldekort(
            meldekortId = 5001,
            personId = 100,
            periode = periode,
            ukenrUke1 = 10,
            ukenrUke2 = 11,
            meldedato = null,
            meldeform = "E1",
            fortsattRegistrertArbeidssoker = true,
            kommentar = null,
            dager = listOf(
                // Dag 1 er en straffedag (levert for sent) — timer her skal ignoreres.
                MeldekortDag(10, 1, LocalDate.of(2023, 1, 2), 7.5, false),
                // Dag 2 er aktiv og teller med.
                MeldekortDag(10, 2, LocalDate.of(2023, 1, 3), 7.5, false),
            ),
            reduksjon = MeldekortReduksjon(dagerForSent = 1, fravar = 0.0f, sykedager = 0.0f),
        )

        every { meldekortRepository.hentForSak(sakId) } returns MeldekortForSak(
            posteringer = listOf(
                MeldekortPostering(
                    vedtakId = 90010, personId = 100, meldekortId = 5001, periode = periode,
                    belop = 4970, dagsatsMedBarnetillegg = 1812, dagsats = 1500, dagsatsForSamordning = 1500,
                    insGrad = null,
                ),
            ),
            meldekort = listOf(meldekort),
        )
        every { telleverkService.hentKvoteBrukHendelserForPerson(PersonId(100)) } returns emptySet()

        val rad = service.hentTilkjenteYtelserForSak(sakId).rader.single()

        // Kun dag 2 (7,5 timer) teller. Grunnlag = (10 - 1) arbeidsdager à 7,5 timer = 67,5 timer.
        // 7,5 / 67,5 = 11,1 % → 11 %.
        assertThat(rad.timerArbeidet).isEqualTo(7.5)
        assertThat(rad.reduksjon?.levertForSentDager).isEqualTo(1)
        assertThat(rad.reduksjon?.timerArbeidetProsent).isEqualTo(11)
        assertThat(rad.reduksjon?.samordningsProsent).isEqualTo(0)
        assertThat(rad.reduksjon?.totalReduksjonProsent).isEqualTo(11)
        assertThat(rad.reduksjon?.fravar).isEqualTo(0.0f)
        assertThat(rad.reduksjon?.sykedager).isEqualTo(0.0f)
        assertThat(rad.reduksjon?.institusjonsProsent).isNull()
    }

    @Test
    fun `samordningsprosent beregnes fra dagsats foer og etter samordning`() {
        val sakId = SakId(9001)
        val periode = Periode(LocalDate.of(2023, 1, 2), LocalDate.of(2023, 1, 15))
        val meldekort = Meldekort(
            meldekortId = 5001,
            personId = 100,
            periode = periode,
            ukenrUke1 = 10,
            ukenrUke2 = 11,
            meldedato = null,
            meldeform = "E1",
            fortsattRegistrertArbeidssoker = true,
            kommentar = null,
            dager = emptyList(),
            reduksjon = MeldekortReduksjon(dagerForSent = 0, fravar = 0.0f, sykedager = 0.0f),
        )

        every { meldekortRepository.hentForSak(sakId) } returns MeldekortForSak(
            posteringer = listOf(
                MeldekortPostering(
                    vedtakId = 90010, personId = 100, meldekortId = 5001, periode = periode,
                    belop = 4970, dagsatsMedBarnetillegg = 1200, dagsats = 800, dagsatsForSamordning = 1000,
                    insGrad = 50,
                ),
            ),
            meldekort = listOf(meldekort),
        )
        every { telleverkService.hentKvoteBrukHendelserForPerson(PersonId(100)) } returns emptySet()

        val rad = service.hentTilkjenteYtelserForSak(sakId).rader.single()

        // (1000 - 800) / 1000 = 20 %. Ingen arbeid. insGrad = 50 → total = 0 + 20 + 50 = 70 %.
        assertThat(rad.reduksjon?.timerArbeidetProsent).isEqualTo(0)
        assertThat(rad.reduksjon?.samordningsProsent).isEqualTo(20)
        assertThat(rad.reduksjon?.totalReduksjonProsent).isEqualTo(70)
        assertThat(rad.reduksjon?.fravar).isEqualTo(0.0f)
        assertThat(rad.reduksjon?.sykedager).isEqualTo(0.0f)
        assertThat(rad.reduksjon?.institusjonsProsent).isEqualTo(50)
    }

    @Test
    fun `gjenstaaende kvote videreføres for meldekort uten egen bevegelse paa kvotetypen`() {
        val sakId = SakId(9001)
        val periodeEn = Periode(LocalDate.of(2023, 1, 2), LocalDate.of(2023, 1, 15))
        val periodeTo = Periode(LocalDate.of(2023, 1, 16), LocalDate.of(2023, 1, 29))

        every { meldekortRepository.hentForSak(sakId) } returns MeldekortForSak(
            posteringer = listOf(
                MeldekortPostering(
                    vedtakId = 90010, personId = 100, meldekortId = 5001, periode = periodeEn,
                    belop = 7700, dagsatsMedBarnetillegg = 550, dagsats = 520, dagsatsForSamordning = 520,
                    insGrad = null,
                ),
                MeldekortPostering(
                    vedtakId = 90010, personId = 100, meldekortId = 5002, periode = periodeTo,
                    belop = 6600, dagsatsMedBarnetillegg = 550, dagsats = 520, dagsatsForSamordning = 520,
                    insGrad = null,
                ),
            ),
            meldekort = emptyList(),
        )
        every { telleverkService.hentKvoteBrukHendelserForPerson(PersonId(100)) } returns setOf(
            kvotebrukHendelse(id = 200, kvoteTypeKode = "AAP", grunnlag = "VEDTAK", objektId = 90010, resterende = 20),
            kvotebrukHendelse(id = 201, kvoteTypeKode = "MAAPU", grunnlag = "VEDTAK", objektId = 90010, resterende = 30),
            kvotebrukHendelse(id = 202, kvoteTypeKode = "AAP", grunnlag = "MKORT", objektId = 5001, resterende = 10),
            kvotebrukHendelse(id = 203, kvoteTypeKode = "AAP", grunnlag = "MKORT", objektId = 5002, resterende = 4),
            kvotebrukHendelse(id = 204, kvoteTypeKode = "MAAPU", grunnlag = "MKORT", objektId = 5002, resterende = 25),
        )

        val rader = service.hentTilkjenteYtelserForSak(sakId).rader

        val førsteRad = rader.first { it.fraOgMedDato == periodeEn.fraOgMedDato }
        assertThat(førsteRad.gjenstaaendeOrdinaerDager).isEqualTo(10)
        assertThat(førsteRad.gjenstaaendeUnntakDager).isEqualTo(30)

        val andreRad = rader.first { it.fraOgMedDato == periodeTo.fraOgMedDato }
        assertThat(andreRad.gjenstaaendeOrdinaerDager).isEqualTo(4)
        assertThat(andreRad.gjenstaaendeUnntakDager).isEqualTo(25)
    }

    @Test
    fun `betalingsplan og ukjent kilde tas med som egne rader`() {
        val sakId = SakId(9004)
        val periodeEn = Periode(LocalDate.of(2023, 2, 27), LocalDate.of(2023, 3, 12))
        val periodeTo = Periode(LocalDate.of(2023, 3, 13), LocalDate.of(2023, 3, 26))

        every { meldekortRepository.hentForSak(sakId) } returns MeldekortForSak(
            posteringer = listOf(
                MeldekortPostering(
                    vedtakId = 90040, personId = 104, meldekortId = null, periode = periodeEn,
                    belop = 2500, dagsatsMedBarnetillegg = null, dagsats = null, dagsatsForSamordning = null,
                    insGrad = null, kilde = PosteringKilde.BETALINGSPLAN, kildeAlias = "BETPLAN",
                    kildeObjektId = 6600004,
                ),
                // Ukjent alias skal ikke tolkes som spesialutbetaling — den blir UKJENT.
                MeldekortPostering(
                    vedtakId = 90040, personId = 104, meldekortId = null, periode = periodeTo,
                    belop = 1200, dagsatsMedBarnetillegg = null, dagsats = null, dagsatsForSamordning = null,
                    insGrad = null, kilde = PosteringKilde.UKJENT, kildeAlias = null, kildeObjektId = null,
                ),
            ),
            meldekort = emptyList(),
        )
        every { telleverkService.hentKvoteBrukHendelserForPerson(PersonId(104)) } returns emptySet()

        val rader = service.hentTilkjenteYtelserForSak(sakId).rader

        assertThat(rader.map { it.kilde })
            .containsExactly(PosteringKilde.BETALINGSPLAN, PosteringKilde.UKJENT)
        assertThat(rader.map { it.beregnetBrutto }).containsExactly(2500, 1200)
        assertThat(rader.all { it.meldekort == null && it.uke == null }).isTrue()
    }

    @Test
    fun `returnerer tom respons for sak uten posteringer`() {
        val sakId = SakId(123)
        every { meldekortRepository.hentForSak(sakId) } returns MeldekortForSak(emptyList(), emptyList())

        val response = service.hentTilkjenteYtelserForSak(sakId)

        assertThat(response.rader).isEmpty()
    }

    @Test
    fun `andre kall for samme sak besvares fra cache`() {
        val sakId = SakId(456)
        val periode = Periode(LocalDate.of(2023, 1, 2), LocalDate.of(2023, 1, 15))
        every { meldekortRepository.hentForSak(sakId) } returns MeldekortForSak(
            posteringer = listOf(
                MeldekortPostering(
                    vedtakId = 90010, personId = 100, meldekortId = 5001, periode = periode,
                    belop = 7700, dagsatsMedBarnetillegg = 550, dagsats = 520, dagsatsForSamordning = 520,
                    insGrad = null,
                ),
            ),
            meldekort = emptyList(),
        )
        every { telleverkService.hentKvoteBrukHendelserForPerson(PersonId(100)) } returns emptySet()

        val forste = service.hentTilkjenteYtelserForSak(sakId)
        val andre = service.hentTilkjenteYtelserForSak(sakId)

        assertThat(andre).isEqualTo(forste)
        verify(exactly = 1) { meldekortRepository.hentForSak(sakId) }
        verify(exactly = 1) { telleverkService.hentKvoteBrukHendelserForPerson(PersonId(100)) }
    }

    @Test
    fun `cacher uavhengig per sak`() {
        val sakEn = SakId(457)
        val sakTo = SakId(458)
        every { meldekortRepository.hentForSak(sakEn) } returns MeldekortForSak(emptyList(), emptyList())
        every { meldekortRepository.hentForSak(sakTo) } returns MeldekortForSak(emptyList(), emptyList())

        assertThat(service.hentTilkjenteYtelserForSak(sakEn).sakId).isEqualTo(457)
        assertThat(service.hentTilkjenteYtelserForSak(sakTo).sakId).isEqualTo(458)

        verify(exactly = 1) { meldekortRepository.hentForSak(sakEn) }
        verify(exactly = 1) { meldekortRepository.hentForSak(sakTo) }
    }

    private fun kvotebrukHendelse(
        id: Int,
        kvoteTypeKode: String,
        grunnlag: String,
        objektId: Long,
        resterende: Int,
    ) = KvotebrukHendelse(
        id = id,
        kvoteTypeKode = kvoteTypeKode,
        endringsGrunnlag = grunnlag,
        objektIdGrunnlag = objektId,
        antallBevegelse = 0,
        posteringTypeKode = "OPPD",
        datoHendelse = LocalDate.of(2023, 1, 1),
        resterende = resterende,
        modUser = null,
        begrunnelse = null,
    )
}
