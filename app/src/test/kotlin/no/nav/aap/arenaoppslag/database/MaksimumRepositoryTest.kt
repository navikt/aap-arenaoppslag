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
    fun `henter meldekortdata per utbetalingsperiode`() {
        val utbetalinger = repo.hentMaksimumsløsning(fnrMedVedtak, søkeperiodeFra, søkeperiodeTil)
            .vedtak.first().utbetaling

        val utbetaling1 = utbetalinger.find { it.periode.fraOgMedDato == LocalDate.of(2023, 1, 2) }!!
        val utbetaling2 = utbetalinger.find { it.periode.fraOgMedDato == LocalDate.of(2023, 1, 16) }!!

        // meldekort 5001: FSNN=1, ingen SENN/FXNN
        val meldekortdata1 = utbetaling1.reduksjon!!.annenReduksjon
        assertThat(meldekortdata1.sykedager).isEqualTo(1.0f)
        assertThat(meldekortdata1.sentMeldekort).isFalse()
        assertThat(meldekortdata1.fraver).isEqualTo(0.0f)

        // meldekort 5002: SENN=1, FXNN=2, ingen FSNN
        val meldekortdata2 = utbetaling2.reduksjon!!.annenReduksjon
        assertThat(meldekortdata2.sykedager).isEqualTo(0.0f)
        assertThat(meldekortdata2.sentMeldekort).isTrue()
        assertThat(meldekortdata2.fraver).isEqualTo(2.0f)
    }
}
