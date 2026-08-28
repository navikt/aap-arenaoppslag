package no.nav.aap.arenaoppslag.database

import no.nav.aap.arenaoppslag.modeller.PosteringKilde
import no.nav.aap.arenaoppslag.modeller.SakId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class MeldekortRepositoryTest : H2TestBase("flyway/maksimum") {

    private val repo = MeldekortRepository(h2)

    // Sak 9001 har vedtak 90010 med to posteringer og to meldekort (5001, 5002).
    private val sakMedMeldekort = SakId(9001)

    // Sak 9004 har én postering per kildevariant (MKORT, SPESUTB, BETPLAN og manglende alias).
    private val sakMedKildevarianter = SakId(9004)

    // Sak 9006 har meldekort både med og uten postering, samt et dagpenge-meldekort.
    private val sakMedMeldekortUtenPostering = SakId(9006)

    // Sak 9007 tilhører samme person som 9006 og eier meldekort 7005.
    private val annenSakForSammePerson = SakId(9007)
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
        assertThat(første.insGrad).isEqualTo(33)
        assertThat(første.kilde).isEqualTo(PosteringKilde.MELDEKORT)
        assertThat(første.kildeObjektId).isEqualTo(5001)
    }

    @Test
    fun `utleder kilde fra tabellnavnalias_kilde`() {
        val posteringer = repo.hentForSak(sakMedKildevarianter).posteringer

        assertThat(posteringer).hasSize(4)
        val kildePerBelop = posteringer.associate { it.belop to it.kilde }
        assertThat(kildePerBelop[7700]).isEqualTo(PosteringKilde.MELDEKORT)
        assertThat(kildePerBelop[3459]).isEqualTo(PosteringKilde.SPESIALUTBETALING)
        assertThat(kildePerBelop[2500]).isEqualTo(PosteringKilde.BETALINGSPLAN)
        // Uten alias vet vi ikke kilden — den skal ikke gjettes til spesialutbetaling.
        assertThat(kildePerBelop[1200]).isEqualTo(PosteringKilde.UKJENT)

        val spesialutbetaling = posteringer.first { it.belop == 3459 }
        assertThat(spesialutbetaling.meldekortId).isNull()
        assertThat(spesialutbetaling.kildeAlias).isEqualTo("SPESUTB")
        assertThat(spesialutbetaling.kildeObjektId).isEqualTo(7700004)

        val utenAlias = posteringer.first { it.belop == 1200 }
        assertThat(utenAlias.kildeAlias).isNull()
        assertThat(utenAlias.kildeObjektId).isNull()
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
    fun `meldekortdager over aarsskiftet faar riktig dato`() {
        // Sak 9005 har et meldekort med uke 52 (2022) og uke 1 (2023).
        val meldekort = repo.hentForSak(SakId(9005)).meldekort.first { it.meldekortId == 6501L }

        assertThat(meldekort.periode.fraOgMedDato).isEqualTo(LocalDate.of(2022, 12, 26))
        val mandagUke52 = meldekort.dager.first { it.ukenr == 52 && it.dagnr == 1 }
        assertThat(mandagUke52.dato).isEqualTo(LocalDate.of(2022, 12, 26))
        val søndagUke52 = meldekort.dager.first { it.ukenr == 52 && it.dagnr == 7 }
        assertThat(søndagUke52.dato).isEqualTo(LocalDate.of(2023, 1, 1))
        val mandagUke1 = meldekort.dager.first { it.ukenr == 1 && it.dagnr == 1 }
        assertThat(mandagUke1.dato).isEqualTo(LocalDate.of(2023, 1, 2))
        val søndagUke1 = meldekort.dager.first { it.ukenr == 1 && it.dagnr == 7 }
        assertThat(søndagUke1.dato).isEqualTo(LocalDate.of(2023, 1, 8))
    }

    @Test
    fun `meldekort henter anmerkninger som reduksjon`() {
        val resultat = repo.hentForSak(sakMedMeldekort)

        val meldekort5001 = resultat.meldekort.first { it.meldekortId == 5001L }
        assertThat(meldekort5001.reduksjon.dagerForSent).isEqualTo(0)
        assertThat(meldekort5001.reduksjon.fravar).isEqualTo(0.0f)
        assertThat(meldekort5001.reduksjon.sykedager).isEqualTo(1.0f)

        val meldekort5002 = resultat.meldekort.first { it.meldekortId == 5002L }
        assertThat(meldekort5002.reduksjon.dagerForSent).isEqualTo(1)
        assertThat(meldekort5002.reduksjon.fravar).isEqualTo(2.0f)
        assertThat(meldekort5002.reduksjon.sykedager).isEqualTo(0.0f)
    }

    @Test
    fun `meldekort henter alle anmerkninger med navn og beskrivelse fra kodetabellen`() {
        val resultat = repo.hentForSak(sakMedMeldekort)

        val meldekort5001 = resultat.meldekort.first { it.meldekortId == 5001L }
        // MAXAA påvirker ikke reduksjonen, men skal fortsatt eksponeres.
        assertThat(meldekort5001.anmerkninger.map { it.kode }).containsExactly("FSNN", "MAXAA")

        val sykdom = meldekort5001.anmerkninger.first { it.kode == "FSNN" }
        assertThat(sykdom.navn).isEqualTo("Fravær av type S")
        assertThat(sykdom.beskrivelse).isEqualTo("Utbetalingen er redusert pga sykdom &1 dager")
        assertThat(sykdom.verdi).isEqualTo(1)
        assertThat(sykdom.verdi2).isNull()

        val maksperiode = meldekort5001.anmerkninger.first { it.kode == "MAXAA" }
        assertThat(maksperiode.navn).isEqualTo("Maks periode AAP")
        assertThat(maksperiode.verdi).isNull()

        val meldekort5002 = resultat.meldekort.first { it.meldekortId == 5002L }
        assertThat(meldekort5002.anmerkninger.map { it.kode }).containsExactly("SENN", "FXNN")
    }

    @Test
    fun `henter meldekort uten postering innenfor sakens vedtaksvindu`() {
        val meldekort = repo.hentForSak(sakMedMeldekortUtenPostering).meldekort

        // 7001 har postering, 7002 og 7003 har ikke. 7004 er dagpenger og 7005 er postert på sak 9007.
        assertThat(meldekort.map { it.meldekortId }).containsExactly(7001L, 7002L, 7003L)
    }

    @Test
    fun `beregningstatus foelger med paa meldekortet`() {
        val meldekort = repo.hentForSak(sakMedMeldekortUtenPostering).meldekort

        assertThat(meldekort.first { it.meldekortId == 7002L }.beregningStatusKode).isEqualTo("FERDI")
        assertThat(meldekort.first { it.meldekortId == 7003L }.beregningStatusKode).isEqualTo("OPPRE")
    }

    @Test
    fun `meldekort uten postering har dager og anmerkninger`() {
        val meldekort = repo.hentForSak(sakMedMeldekortUtenPostering).meldekort
            .first { it.meldekortId == 7002L }

        assertThat(meldekort.dager.sumOf { it.timerArbeidet }).isEqualTo(15.0)
        assertThat(meldekort.reduksjon.fravar).isEqualTo(10.0f)
        assertThat(meldekort.periode.fraOgMedDato).isEqualTo(LocalDate.of(2023, 3, 27))
    }

    @Test
    fun `meldekort postert paa annen sak hoerer til den saken`() {
        val resultat = repo.hentForSak(annenSakForSammePerson)

        // 7005 er postert her. 7002 og 7003 ligger utenfor vedtaksvinduet til 9007.
        assertThat(resultat.meldekort.map { it.meldekortId }).containsExactly(7005L)
        assertThat(resultat.posteringer.map { it.meldekortId }).containsExactly(7005L)
    }
}


