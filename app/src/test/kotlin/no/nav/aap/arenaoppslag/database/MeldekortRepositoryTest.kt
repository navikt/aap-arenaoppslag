package no.nav.aap.arenaoppslag.database

import no.nav.aap.arenaoppslag.modeller.SakId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class MeldekortRepositoryTest : H2TestBase("flyway/maksimum") {

    private val repo = MeldekortRepository(h2)

    // Sak 9001 har vedtak 90010 med to posteringer og to meldekort (5001, 5002).
    private val sakMedMeldekort = SakId(9001)
    private val ukjentSak = SakId(99999)

    @Test
    fun `returnerer tomt resultat for ukjent sak`() {
        val resultat = repo.hentForSak(ukjentSak)

        assertThat(resultat.posteringer).isEmpty()
        assertThat(resultat.meldekort).isEmpty()
    }

    @Test
    fun `henter posteringer for sak`() {
        val resultat = repo.hentForSak(sakMedMeldekort)

        assertThat(resultat.posteringer).hasSize(2)
        val første = resultat.posteringer.first { it.periode.fraOgMedDato == LocalDate.of(2023, 1, 2) }
        assertThat(første.belop).isEqualTo(7700)
        assertThat(første.meldekortId).isEqualTo(5001)
        assertThat(første.vedtakId).isEqualTo(90010)
        assertThat(første.periode.tilOgMedDato).isEqualTo(LocalDate.of(2023, 1, 15))
        assertThat(første.dagsatsMedBarnetillegg).isEqualTo(550)
        assertThat(første.dagsats).isEqualTo(520)
        assertThat(første.dagsatsForSamordning).isEqualTo(520)
    }

    @Test
    fun `henter meldekort med periode og meldeform`() {
        val resultat = repo.hentForSak(sakMedMeldekort)

        assertThat(resultat.meldekort).hasSize(2)
        val meldekort = resultat.meldekort.first { it.meldekortId == 5001L }
        assertThat(meldekort.meldeform).isEqualTo("E1")
        assertThat(meldekort.periode.fraOgMedDato).isEqualTo(LocalDate.of(2023, 1, 2))
        assertThat(meldekort.ukenrUke1).isEqualTo(1)
        assertThat(meldekort.ukenrUke2).isEqualTo(2)
    }

    @Test
    fun `meldekortdager har utledet dato og timer arbeidet`() {
        val resultat = repo.hentForSak(sakMedMeldekort)

        val meldekort = resultat.meldekort.first { it.meldekortId == 5001L }
        // Testdata har dag 1-5 for uke 1 og uke 2 = 10 dager.
        assertThat(meldekort.dager).hasSize(10)
        val mandagUke1 = meldekort.dager.first { it.ukenr == 1 && it.dagnr == 1 }
        assertThat(mandagUke1.dato).isEqualTo(LocalDate.of(2023, 1, 2))
        assertThat(mandagUke1.timerArbeidet).isEqualTo(3.0)
        // Dag 1 i uke 2 er én uke etter periodestart.
        val mandagUke2 = meldekort.dager.first { it.ukenr == 2 && it.dagnr == 1 }
        assertThat(mandagUke2.dato).isEqualTo(LocalDate.of(2023, 1, 9))
    }

    @Test
    fun `meldekort henter anmerkninger som reduksjon`() {
        val resultat = repo.hentForSak(sakMedMeldekort)

        val meldekort5001 = resultat.meldekort.first { it.meldekortId == 5001L }
        assertThat(meldekort5001.reduksjon.dagerForSent).isEqualTo(0)
        assertThat(meldekort5001.reduksjon.fravar).isEqualTo(0.0f)

        val meldekort5002 = resultat.meldekort.first { it.meldekortId == 5002L }
        assertThat(meldekort5002.reduksjon.dagerForSent).isEqualTo(1)
        assertThat(meldekort5002.reduksjon.fravar).isEqualTo(2.0f)
    }
}


