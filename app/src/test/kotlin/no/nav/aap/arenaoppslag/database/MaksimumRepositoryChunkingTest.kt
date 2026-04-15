package no.nav.aap.arenaoppslag.database

import no.nav.aap.arenaoppslag.modeller.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class MaksimumRepositoryChunkingTest : H2TestBase("flyway/maksimum") {

    // chunk-størrelse 1 tvinger ett databasekall per meldekort-id — verifiserer at chunking og sammenslåing
    // av resultater fungerer korrekt selv når lista splittes i mange små biter
    private val repo = MaksimumRepository(h2, chunkStørrelse = 1)

    private val fnrMedVedtak = "12345678901"
    private val søkeperiodeFra = LocalDate.of(2023, 1, 1)
    private val søkeperiodeTil = LocalDate.of(2023, 12, 31)

    @Test
    fun `chunking slår korrekt sammen anmerkninger fra flere chunks`() {
        val utbetalinger = repo.hentMaksimumsløsning(fnrMedVedtak, søkeperiodeFra, søkeperiodeTil)
            .vedtak.first().utbetaling

        assertThat(utbetalinger).hasSize(2)

        // meldekort 5001: FSNN=1 (sykedag)
        val utbetaling1 = utbetalinger.find { it.periode.fraOgMedDato == LocalDate.of(2023, 1, 2) }!!
        assertThat(utbetaling1.reduksjon!!.annenReduksjon.sykedager).isEqualTo(1.0f)
        assertThat(utbetaling1.reduksjon!!.annenReduksjon.sentMeldekort).isFalse()

        // meldekort 5002: SENN=1 (for sent) og FXNN=2 (fravær)
        val utbetaling2 = utbetalinger.find { it.periode.fraOgMedDato == LocalDate.of(2023, 1, 16) }!!
        assertThat(utbetaling2.reduksjon!!.annenReduksjon.sentMeldekort).isTrue()
        assertThat(utbetaling2.reduksjon!!.annenReduksjon.fraver).isEqualTo(2.0f)
    }
}
