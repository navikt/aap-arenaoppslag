package no.nav.aap.arenaoppslag.database

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
}
