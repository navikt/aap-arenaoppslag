package no.nav.aap.arenaoppslag.service

import io.mockk.every
import io.mockk.mockk
import no.nav.aap.arenaoppslag.database.MeldekortRepository
import no.nav.aap.arenaoppslag.database.TelleverkRepository
import no.nav.aap.arenaoppslag.modeller.AnnenReduksjon
import no.nav.aap.arenaoppslag.modeller.KvoteVerdi
import no.nav.aap.arenaoppslag.modeller.Meldekort
import no.nav.aap.arenaoppslag.modeller.MeldekortDag
import no.nav.aap.arenaoppslag.modeller.MeldekortForSak
import no.nav.aap.arenaoppslag.modeller.MeldekortPostering
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
            reduksjon = AnnenReduksjon(0.0f, true, 0.0f),
        )

        every { meldekortRepository.hentForSak(sakId) } returns MeldekortForSak(
            posteringer = listOf(
                MeldekortPostering(90010, 100, 5001, periode, 4970, 1812, 1500),
                MeldekortPostering(90010, 100, null, periode, 3459, null, null),
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
        assertThat(meldekortRad.reduksjon?.levertForSent).isTrue()
        assertThat(meldekortRad.meldekort?.uker).hasSize(1)
        assertThat(meldekortRad.meldekort?.fortsattRegistrertArbeidssoker).isTrue()

        val spesialRad = response.rader.first { it.kilde == "Spesialutbetaling" }
        assertThat(spesialRad.uke).isNull()
        assertThat(spesialRad.meldekort).isNull()
        assertThat(spesialRad.beregnetBrutto).isEqualTo(3459)
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


