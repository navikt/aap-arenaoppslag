package no.nav.aap.arenaoppslag.service

import io.mockk.every
import io.mockk.mockk
import no.nav.aap.arenaoppslag.database.MeldekortRepository
import no.nav.aap.arenaoppslag.database.TelleverkRepository
import no.nav.aap.arenaoppslag.modeller.KvoteVerdi
import no.nav.aap.arenaoppslag.modeller.Meldekort
import no.nav.aap.arenaoppslag.modeller.MeldekortDag
import no.nav.aap.arenaoppslag.modeller.MeldekortForSak
import no.nav.aap.arenaoppslag.modeller.MeldekortPostering
import no.nav.aap.arenaoppslag.modeller.MeldekortReduksjon
import no.nav.aap.arenaoppslag.modeller.Periode
import no.nav.aap.arenaoppslag.modeller.PersonId
import no.nav.aap.arenaoppslag.modeller.SakId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TilkjentYtelserServiceTest {

    private val meldekortRepository = mockk<MeldekortRepository>()
    private val telleverkRepository = mockk<TelleverkRepository>()

    private val service = TilkjentYtelserService(meldekortRepository, telleverkRepository)

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
            reduksjon = MeldekortReduksjon(dagerForSent = 0, fravar = 0.0f),
        )

        every { meldekortRepository.hentForSak(sakId) } returns MeldekortForSak(
            posteringer = listOf(
                MeldekortPostering(
                    vedtakId = 90010, personId = 100, meldekortId = 5001, periode = periode,
                    belop = 4970, dagsatsMedBarnetillegg = 1812, dagsats = 1500, dagsatsForSamordning = 1500,
                ),
                MeldekortPostering(
                    vedtakId = 90010, personId = 100, meldekortId = null, periode = periode,
                    belop = 3459, dagsatsMedBarnetillegg = null, dagsats = null, dagsatsForSamordning = null,
                ),
            ),
            meldekort = listOf(meldekort),
        )
        every { telleverkRepository.hentTelleverkForPerson(PersonId(100)) } returns setOf(
            KvoteVerdi("AAP", 2),
            KvoteVerdi("MAAPU", 704),
        )

        val response = service.hentTilkjenteYtelserForSak(sakId)

        assertThat(response.sakId).isEqualTo(9001)
        assertThat(response.gjenstaaendeOrdinaerDager).isEqualTo(2)
        assertThat(response.gjenstaaendeUnntakDager).isEqualTo(704)
        assertThat(response.rader).hasSize(2)

        val meldekortRad = response.rader.first { it.kilde == "Meldekort" }
        assertThat(meldekortRad.uke).isEqualTo("10-11")
        assertThat(meldekortRad.dagsatsMedBarnetillegg).isEqualTo(1812)
        assertThat(meldekortRad.dagsats).isEqualTo(1500)
        assertThat(meldekortRad.beregnetBrutto).isEqualTo(4970)
        assertThat(meldekortRad.timerArbeidet).isEqualTo(7.5)
        // 7,5 timer av 14 dager à 7,5 timer = 7,5 / 105 = 7 %. Ingen samordning (DAGS == DAGSFSAM).
        assertThat(meldekortRad.reduksjon?.levertForSentDager).isEqualTo(0)
        assertThat(meldekortRad.reduksjon?.timerArbeidetProsent).isEqualTo(7)
        assertThat(meldekortRad.reduksjon?.samordningsProsent).isEqualTo(0)
        assertThat(meldekortRad.reduksjon?.totalReduksjonProsent).isEqualTo(7)
        assertThat(meldekortRad.meldekort?.uker).hasSize(1)
        assertThat(meldekortRad.meldekort?.fortsattRegistrertArbeidssoker).isTrue()

        val spesialRad = response.rader.first { it.kilde == "Spesialutbetaling" }
        assertThat(spesialRad.uke).isNull()
        assertThat(spesialRad.meldekort).isNull()
        assertThat(spesialRad.beregnetBrutto).isEqualTo(3459)
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
            reduksjon = MeldekortReduksjon(dagerForSent = 1, fravar = 0.0f),
        )

        every { meldekortRepository.hentForSak(sakId) } returns MeldekortForSak(
            posteringer = listOf(
                MeldekortPostering(
                    vedtakId = 90010, personId = 100, meldekortId = 5001, periode = periode,
                    belop = 4970, dagsatsMedBarnetillegg = 1812, dagsats = 1500, dagsatsForSamordning = 1500,
                ),
            ),
            meldekort = listOf(meldekort),
        )
        every { telleverkRepository.hentTelleverkForPerson(PersonId(100)) } returns emptySet()

        val rad = service.hentTilkjenteYtelserForSak(sakId).rader.single()

        // Kun dag 2 (7,5 timer) teller. Grunnlag = (14 - 1) dager à 7,5 timer = 97,5 timer.
        // 7,5 / 97,5 = 7,69 % → 8 %.
        assertThat(rad.timerArbeidet).isEqualTo(7.5)
        assertThat(rad.reduksjon?.levertForSentDager).isEqualTo(1)
        assertThat(rad.reduksjon?.timerArbeidetProsent).isEqualTo(8)
        assertThat(rad.reduksjon?.samordningsProsent).isEqualTo(0)
        assertThat(rad.reduksjon?.totalReduksjonProsent).isEqualTo(8)
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
            reduksjon = MeldekortReduksjon(dagerForSent = 0, fravar = 0.0f),
        )

        every { meldekortRepository.hentForSak(sakId) } returns MeldekortForSak(
            posteringer = listOf(
                MeldekortPostering(
                    vedtakId = 90010, personId = 100, meldekortId = 5001, periode = periode,
                    belop = 4970, dagsatsMedBarnetillegg = 1200, dagsats = 800, dagsatsForSamordning = 1000,
                ),
            ),
            meldekort = listOf(meldekort),
        )
        every { telleverkRepository.hentTelleverkForPerson(PersonId(100)) } returns emptySet()

        val rad = service.hentTilkjenteYtelserForSak(sakId).rader.single()

        // (1000 - 800) / 1000 = 20 %. Ingen arbeid → total = 20 %.
        assertThat(rad.reduksjon?.timerArbeidetProsent).isEqualTo(0)
        assertThat(rad.reduksjon?.samordningsProsent).isEqualTo(20)
        assertThat(rad.reduksjon?.totalReduksjonProsent).isEqualTo(20)
    }

    @Test
    fun `returnerer tom respons uten kvoter for sak uten posteringer`() {
        val sakId = SakId(123)
        every { meldekortRepository.hentForSak(sakId) } returns MeldekortForSak(emptyList(), emptyList())

        val response = service.hentTilkjenteYtelserForSak(sakId)

        assertThat(response.rader).isEmpty()
        assertThat(response.gjenstaaendeOrdinaerDager).isNull()
        assertThat(response.gjenstaaendeUnntakDager).isNull()
    }
}


