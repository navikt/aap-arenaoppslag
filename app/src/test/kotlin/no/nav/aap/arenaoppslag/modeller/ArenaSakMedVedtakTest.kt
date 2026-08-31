package no.nav.aap.arenaoppslag.modeller

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class ArenaSakMedVedtakTest {

    private fun sak() = ArenaSakMedVedtak(
        sakId = "9001",
        opprettetAar = 2023,
        lopenr = 1,
        person = ArenaSakPerson(100, "12345678901", "Test", "Testesen"),
        statuskode = "INAKT",
        statusnavn = "Inaktiv",
        registrertDato = LocalDateTime.of(2023, 1, 1, 0, 0),
        avsluttetDato = null,
        vedtak = emptyList(),
    )

    @Test
    fun `tilKontrakt inkluderer tilkjent ytelse naar den er satt`() {
        val tilkjentYtelse = TilkjentYtelseResponse(9001, emptyList())

        val detaljert = sak().tilKontrakt(
            telleverkForPerson = null,
            kvoteHistorikk = emptySet(),
            sisteUtbetalingDato = null,
            maksdato = null,
            oppgaver = emptyList(),
            tilkjentYtelse = tilkjentYtelse,
        )

        assertThat(detaljert.tilkjentYtelse).isEqualTo(tilkjentYtelse)
    }

    @Test
    fun `tilKontrakt setter tilkjent ytelse til null som standard`() {
        val detaljert = sak().tilKontrakt(
            telleverkForPerson = null,
            kvoteHistorikk = emptySet(),
            sisteUtbetalingDato = null,
            maksdato = null,
            oppgaver = emptyList(),
            )

        assertThat(detaljert.tilkjentYtelse).isNull()
    }
}

