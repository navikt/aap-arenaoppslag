package no.nav.aap.arenaoppslag.database

import no.nav.aap.arenaoppslag.modeller.AnnenReduksjon
import no.nav.aap.arenaoppslag.modeller.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class MaksimumRepositorySummeringTest : H2TestBase("flyway/maksimumsummering") {
    private val repo = MaksimumRepository(h2)
    private val fraOgMedDato = LocalDate.of(2023, 1, 1)
    private val tilOgMedDato = LocalDate.of(2023, 12, 31)

    @Test
    fun `summerer timer og beløp per posteringsperiode uten timer fra resten av meldekortet`() {
        val vedtak = repo.hentMaksimumsløsning("12000000000", fraOgMedDato, tilOgMedDato).vedtak.single()

        assertThat(vedtak.periode).isEqualTo(Periode(LocalDate.of(2023, 1, 10), LocalDate.of(2023, 1, 20)))
        assertThat(vedtak.utbetaling).hasSize(3)
        assertThat(vedtak.utbetaling.associate { it.periode to it.belop }).containsExactlyInAnyOrderEntriesOf(
            mapOf(
                Periode(LocalDate.of(2023, 1, 10), LocalDate.of(2023, 1, 11)) to 7700,
                Periode(LocalDate.of(2023, 1, 12), LocalDate.of(2023, 1, 15)) to 6700,
                Periode(LocalDate.of(2023, 1, 16), LocalDate.of(2023, 1, 20)) to 0,
            )
        )
        assertThat(vedtak.utbetaling.associate { it.periode to it.reduksjon!!.timerArbeidet })
            .containsExactlyInAnyOrderEntriesOf(
                mapOf(
                    Periode(LocalDate.of(2023, 1, 10), LocalDate.of(2023, 1, 11)) to 3.0,
                    Periode(LocalDate.of(2023, 1, 12), LocalDate.of(2023, 1, 15)) to 4.0,
                    Periode(LocalDate.of(2023, 1, 16), LocalDate.of(2023, 1, 20)) to 40.0,
                )
            )
    }

    @Test
    fun `beholder arbeidstimer for meldekort med null eller negativt nettobeløp`() {
        val vedtakMedNullbelop = repo.hentMaksimumsløsning("12000000000", fraOgMedDato, tilOgMedDato)
            .vedtak.single()
        val nullutbetaling = vedtakMedNullbelop.utbetaling.single {
            it.periode.fraOgMedDato == LocalDate.of(2023, 1, 16)
        }
        assertThat(nullutbetaling.belop).isZero()
        assertThat(nullutbetaling.reduksjon!!.timerArbeidet).isEqualTo(40.0)

        val vedtakMedNegativtBelop = repo.hentMaksimumsløsning("12100000000", fraOgMedDato, tilOgMedDato)
            .vedtak.single()
        val negativUtbetaling = vedtakMedNegativtBelop.utbetaling.single()
        assertThat(negativUtbetaling.belop).isEqualTo(-1000)
        assertThat(negativUtbetaling.reduksjon!!.timerArbeidet).isEqualTo(3.0)
    }

    @Test
    fun `tar med utbetalinger utenfor søkeperioden når posteringsperioden ligger innenfor vedtaket`() {
        val søkedato = LocalDate.of(2023, 1, 20)
        val utbetaling = repo.hentMaksimumsløsning("12000000000", søkedato, søkedato)
            .vedtak.single().utbetaling.single { it.periode.fraOgMedDato == LocalDate.of(2023, 1, 12) }

        assertThat(utbetaling.periode).isEqualTo(Periode(LocalDate.of(2023, 1, 12), LocalDate.of(2023, 1, 15)))
        assertThat(utbetaling.belop).isEqualTo(6700)
    }

    @Test
    fun `fordeler AAP-posteringer fra ugyldiggjort vedtak etter posteringsperiodene`() {
        val vedtak = repo.hentMaksimumsløsning("12200000000", fraOgMedDato, tilOgMedDato).vedtak
            .associateBy { it.vedtaksId }

        assertThat(vedtak.keys).containsExactlyInAnyOrder("92031", "92032")
        val utbetalingFørRegulering = vedtak.getValue("92031").utbetaling.single()
        val utbetalingEtterRegulering = vedtak.getValue("92032").utbetaling.single()

        assertThat(utbetalingFørRegulering.periode)
            .isEqualTo(Periode(LocalDate.of(2023, 1, 30), LocalDate.of(2023, 2, 5)))
        assertThat(utbetalingFørRegulering.belop).isEqualTo(8000)
        assertThat(utbetalingFørRegulering.dagsats).isEqualTo(600)
        assertThat(utbetalingFørRegulering.barnetillegg).isEqualTo(20)
        assertThat(utbetalingFørRegulering.reduksjon!!.timerArbeidet).isEqualTo(7.5)

        assertThat(utbetalingEtterRegulering.periode)
            .isEqualTo(Periode(LocalDate.of(2023, 2, 6), LocalDate.of(2023, 2, 12)))
        assertThat(utbetalingEtterRegulering.belop).isEqualTo(9000)
        assertThat(utbetalingEtterRegulering.dagsats).isEqualTo(650)
        assertThat(utbetalingEtterRegulering.barnetillegg).isEqualTo(30)
        assertThat(utbetalingEtterRegulering.reduksjon!!.timerArbeidet).isEqualTo(2.5)
    }

    @Test
    fun `tar bare med posteringer til vedtak som overlapper søkeperioden etter regulering`() {
        val søkedato = LocalDate.of(2023, 2, 20)
        val vedtak = repo.hentMaksimumsløsning("12200000000", søkedato, søkedato)
            .vedtak.single()

        assertThat(vedtak.vedtaksId).isEqualTo("92032")
        assertThat(vedtak.utbetaling.single().belop).isEqualTo(9000)
    }

    @Test
    fun `knytter reduksjoner til opprinnelige AAP-beregninger også etter G-regulering`() {
        val vedtak = repo.hentMaksimumsløsning("12200000000", fraOgMedDato, tilOgMedDato)
            .vedtak.associateBy { it.vedtaksId }
        val førRegulering = vedtak.getValue("92031").utbetaling.single()
        val etterRegulering = vedtak.getValue("92032").utbetaling.single()

        assertThat(førRegulering.reduksjon!!.annenReduksjon)
            .isEqualTo(AnnenReduksjon(sykedager = 2.0f, sentMeldekort = true, fravær = 3.0f))
        assertThat(etterRegulering.reduksjon!!.annenReduksjon)
            .isEqualTo(AnnenReduksjon(sykedager = 2.0f, sentMeldekort = true, fravær = 0.0f))
    }

    @Test
    fun `deler meldekort ved G-regulering første mai med riktig sats og timer på hver side`() {
        val vedtak = repo.hentMaksimumsløsning(
            "12300000000", LocalDate.of(2025, 4, 28), LocalDate.of(2025, 5, 11)
        ).vedtak.associateBy { it.vedtaksId }

        assertThat(vedtak.keys).containsExactlyInAnyOrder("92040", "92041")
        val førRegulering = vedtak.getValue("92040")
        val etterRegulering = vedtak.getValue("92041")
        assertThat(førRegulering.justertG).isEqualTo("NyG2024")
        assertThat(etterRegulering.justertG).isEqualTo("NyG2025")

        val førsteUtbetaling = førRegulering.utbetaling.single()
        assertThat(førsteUtbetaling.periode)
            .isEqualTo(Periode(LocalDate.of(2025, 4, 28), LocalDate.of(2025, 4, 30)))
        assertThat(førsteUtbetaling.belop).isEqualTo(1800)
        assertThat(førsteUtbetaling.dagsats).isEqualTo(600)
        assertThat(førsteUtbetaling.reduksjon!!.timerArbeidet).isZero()

        val andreUtbetaling = etterRegulering.utbetaling.single()
        assertThat(andreUtbetaling.periode)
            .isEqualTo(Periode(LocalDate.of(2025, 5, 1), LocalDate.of(2025, 5, 11)))
        assertThat(andreUtbetaling.belop).isEqualTo(4900)
        assertThat(andreUtbetaling.dagsats).isEqualTo(700)
        assertThat(andreUtbetaling.reduksjon!!.timerArbeidet).isEqualTo(2.5)
    }

    @Test
    fun `avgrenser timer ved både vedtaksstart og vedtaksslutt midt i meldekortperiodene`() {
        val utbetalinger = repo.hentMaksimumsløsning(
            "12400000000", LocalDate.of(2024, 9, 1), LocalDate.of(2024, 10, 31)
        ).vedtak.single().utbetaling

        assertThat(utbetalinger).hasSize(2)
        val førsteUtbetaling = utbetalinger.single { it.periode.fraOgMedDato == LocalDate.of(2024, 9, 18) }
        assertThat(førsteUtbetaling.periode.tilOgMedDato).isEqualTo(LocalDate.of(2024, 9, 29))
        assertThat(førsteUtbetaling.belop).isEqualTo(4000)
        assertThat(førsteUtbetaling.reduksjon!!.timerArbeidet).isEqualTo(7.0)

        val sisteUtbetaling = utbetalinger.single { it.periode.fraOgMedDato == LocalDate.of(2024, 9, 30) }
        assertThat(sisteUtbetaling.periode.tilOgMedDato).isEqualTo(LocalDate.of(2024, 10, 10))
        assertThat(sisteUtbetaling.belop).isEqualTo(3000)
        assertThat(sisteUtbetaling.reduksjon!!.timerArbeidet).isEqualTo(4.0)
    }

    @Test
    fun `returnerer nytt G-regulert vedtak uten å videreføre forrige års utbetalinger`() {
        val vedtak = repo.hentMaksimumsløsning(
            "12300000000", LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31)
        ).vedtak.single()

        assertThat(vedtak.vedtaksId).isEqualTo("92042")
        assertThat(vedtak.justertG).isEqualTo("NyG2026")
        assertThat(vedtak.utbetaling).isEmpty()
    }
}
