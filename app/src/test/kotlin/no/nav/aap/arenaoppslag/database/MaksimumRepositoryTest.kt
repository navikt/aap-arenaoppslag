package no.nav.aap.arenaoppslag.database

import no.nav.aap.arenaoppslag.modeller.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class MaksimumRepositoryTest : H2TestBase("flyway/maksimum") {

    private val repo = MaksimumRepository(h2)

    // Person med ett gyldig vedtak, to meldekortperioder, posteringer og meldekortdata
    private val fnrMedVedtak = "12345678901"

    // Person uten vedtak
    private val fnrUtenVedtak = "00000000000"

    // Person med vedtak i 2020 — utenfor standardsøkeperioden 2023
    private val fnrVedtakUtenforPeriode = "11111111111"

    private val søkeperiodeFra = LocalDate.of(2023, 1, 1)
    private val søkeperiodeTil = LocalDate.of(2023, 12, 31)

    @Test
    fun `returnerer tomt vedtakliste for person uten vedtak`() {
        val resultat = repo.hentMaksimumsløsning(fnrUtenVedtak, søkeperiodeFra, søkeperiodeTil)

        assertThat(resultat.vedtak).isEmpty()
    }

    @Test
    fun `returnerer tomt vedtakliste når vedtak er utenfor søkeperioden`() {
        val resultat = repo.hentMaksimumsløsning(fnrVedtakUtenforPeriode, søkeperiodeFra, søkeperiodeTil)

        assertThat(resultat.vedtak).isEmpty()
    }

    @Test
    fun `henter vedtak med korrekte feltverdier`() {
        val resultat = repo.hentMaksimumsløsning(fnrMedVedtak, søkeperiodeFra, søkeperiodeTil)

        assertThat(resultat.vedtak).hasSize(1)
        val vedtak = resultat.vedtak.first()
        assertThat(vedtak.vedtaksId).isEqualTo("90010")
        assertThat(vedtak.status).isEqualTo("IVERK")
        assertThat(vedtak.saksnummer).isEqualTo("9001")
        assertThat(vedtak.dagsats).isEqualTo(520)
        assertThat(vedtak.beregningsgrunnlag).isEqualTo(450000)
        assertThat(vedtak.barnMedStonad).isEqualTo(2)
        assertThat(vedtak.vedtaksTypeKode).isEqualTo("O")
        assertThat(vedtak.periode).isEqualTo(Periode(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31)))
        assertThat(vedtak.justertG).isEqualTo("NyG2024")
        assertThat(vedtak.lopenrvedtak).isEqualTo(1)
        assertThat(vedtak.relatertVedtak).isNull()
    }

    @Test
    fun `henter utbetalinger med korrekt periode, beløp og vedtakfakta`() {
        val utbetalinger = repo.hentMaksimumsløsning(fnrMedVedtak, søkeperiodeFra, søkeperiodeTil)
            .vedtak.first().utbetaling

        assertThat(utbetalinger).hasSize(2)

        val utbetaling1 = utbetalinger.find { it.periode.fraOgMedDato == LocalDate.of(2023, 1, 2) }!!
        assertThat(utbetaling1.periode).isEqualTo(Periode(LocalDate.of(2023, 1, 2), LocalDate.of(2023, 1, 15)))
        assertThat(utbetaling1.belop).isEqualTo(7700)
        assertThat(utbetaling1.dagsats).isEqualTo(550)
        assertThat(utbetaling1.barnetillegg).isEqualTo(30)
    }

    @Test
    fun `henter timer arbeidet per meldekortperiode`() {
        val utbetalinger = repo.hentMaksimumsløsning(fnrMedVedtak, søkeperiodeFra, søkeperiodeTil)
            .vedtak.first().utbetaling

        val utbetaling1 = utbetalinger.find { it.periode.fraOgMedDato == LocalDate.of(2023, 1, 2) }!!
        val utbetaling2 = utbetalinger.find { it.periode.fraOgMedDato == LocalDate.of(2023, 1, 16) }!!

        assertThat(utbetaling1.reduksjon!!.timerArbeidet).isEqualTo(5.0)
        assertThat(utbetaling2.reduksjon!!.timerArbeidet).isEqualTo(4.0)
    }

    @Test
    fun `utbetalinger inneholder ikke duplikater når vi henter vedtak med perioder som går over flere år`() {
        // En test som reproduserer en bug vi opplevde. Vi fikk duplikater av utbetalinger når vi hentet
        // ut vedtak over flere år (f.eks. tilDato = NULL). Årsaken til dette er at MELDEKORTPERIODE har
        // Primary key (periodekode, aar), men vi joinet kun på periodekode.
        val resultat = repo.hentMaksimumsløsning(
            "22222222222",
            LocalDate.of(2023, 1, 1),
            LocalDate.of(2024, 12, 31)
        )

        val vedtak = resultat.vedtak.single()
        assertThat(vedtak.utbetaling).hasSize(1)
    }

    @Test
    fun `henter meldekortdata per utbetalingsperiode`() {
        val utbetalinger = repo.hentMaksimumsløsning(fnrMedVedtak, søkeperiodeFra, søkeperiodeTil)
            .vedtak.first().utbetaling

        val utbetaling1 = utbetalinger.find { it.periode.fraOgMedDato == LocalDate.of(2023, 1, 2) }!!
        val utbetaling2 = utbetalinger.find { it.periode.fraOgMedDato == LocalDate.of(2023, 1, 16) }!!

        // meldekort 5001: FSNN=1, ingen SENN/FXNN
        val meldekortdata1 = utbetaling1.reduksjon!!.annenReduksjon
        assertThat(meldekortdata1.sykedager).isEqualTo(1.0f)
        assertThat(meldekortdata1.sentMeldekort).isFalse()
        assertThat(meldekortdata1.fravær).isEqualTo(0.0f)

        // meldekort 5002: SENN=1, FXNN=2, ingen FSNN
        val meldekortdata2 = utbetaling2.reduksjon!!.annenReduksjon
        assertThat(meldekortdata2.sykedager).isEqualTo(0.0f)
        assertThat(meldekortdata2.sentMeldekort).isTrue()
        assertThat(meldekortdata2.fravær).isEqualTo(2.0f)
    }

    @Test
    fun `beholder hele posteringsperioden over årsskiftet når bare januar er etterspurt`() {
        val vedtak = repo.hentMaksimumsløsning(
            "44444444444",
            LocalDate.of(2023, 1, 1),
            LocalDate.of(2023, 1, 31),
        ).vedtak.single()
        val utbetaling = vedtak.utbetaling.single()

        assertThat(utbetaling.periode).isEqualTo(Periode(LocalDate.of(2022, 12, 26), LocalDate.of(2023, 1, 8)))
        assertThat(utbetaling.belop).isEqualTo(5200)
        assertThat(utbetaling.reduksjon!!.timerArbeidet).isEqualTo(5.0)
    }

    @Test
    fun `fordeler posteringer mellom to saker uten å duplisere en periode som krysser vedtaksstart`() {
        val vedtak = repo.hentMaksimumsløsning("55555555555", søkeperiodeFra, søkeperiodeTil)
            .vedtak.associateBy { it.vedtaksId }

        assertThat(vedtak.keys).containsExactlyInAnyOrder("90060", "90070")
        val utbetalingEldreSak = vedtak.getValue("90070").utbetaling.single()
        val utbetalingNyereSak = vedtak.getValue("90060").utbetaling.single()

        assertThat(utbetalingEldreSak.periode)
            .isEqualTo(Periode(LocalDate.of(2023, 2, 27), LocalDate.of(2023, 3, 12)))
        assertThat(utbetalingEldreSak.belop).isEqualTo(5000)
        assertThat(utbetalingEldreSak.reduksjon!!.timerArbeidet).isEqualTo(3.0)

        assertThat(utbetalingNyereSak.periode)
            .isEqualTo(Periode(LocalDate.of(2023, 3, 13), LocalDate.of(2023, 3, 26)))
        assertThat(utbetalingNyereSak.belop).isEqualTo(6000)
        assertThat(utbetalingNyereSak.reduksjon!!.timerArbeidet).isEqualTo(7.5)
        assertThat(utbetalingNyereSak.dagsats).isEqualTo(600)
    }

    @Test
    fun `blander ikke spesialutbetaling og betalingsplan inn i meldekortets utbetaling`() {
        val vedtak = repo.hentMaksimumsløsning("33333333333", søkeperiodeFra, søkeperiodeTil).vedtak.single()
        val utbetaling = vedtak.utbetaling.single()

        assertThat(utbetaling.periode).isEqualTo(Periode(LocalDate.of(2023, 1, 30), LocalDate.of(2023, 2, 12)))
        assertThat(utbetaling.belop).isEqualTo(7700)
        assertThat(utbetaling.reduksjon!!.timerArbeidet).isEqualTo(7.5)
    }

    @Test
    fun `beholder posteringer uten meldekortdager innenfor posteringsperioden`() {
        val vedtak = repo.hentMaksimumsløsning(
            "99999999999",
            LocalDate.of(2025, 11, 1),
            LocalDate.of(2025, 12, 31),
        ).vedtak.associateBy { it.vedtaksId }

        assertThat(vedtak.keys).containsExactlyInAnyOrder("90090", "90091")
        val førsteUtbetaling = vedtak.getValue("90090").utbetaling.single()
        val andreUtbetaling = vedtak.getValue("90091").utbetaling.single()

        assertThat(førsteUtbetaling.belop).isEqualTo(1461)
        assertThat(førsteUtbetaling.periode)
            .isEqualTo(Periode(LocalDate.of(2025, 11, 24), LocalDate.of(2025, 11, 30)))
        assertThat(andreUtbetaling.belop).isEqualTo(731)
        assertThat(andreUtbetaling.periode)
            .isEqualTo(Periode(LocalDate.of(2025, 12, 1), LocalDate.of(2025, 12, 7)))
    }
}
