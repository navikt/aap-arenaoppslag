package arenaoppslag.ekstern

import arenaoppslag.util.H2TestBase
import java.time.LocalDate

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class EksternDaoMaksimumTest : H2TestBase("flyway/minimumtest") {

    @Test
    fun `ingen vedtaks-perioder blir hentet ut for personer som ikke er tilknyttet noe vedtak`() {
        val alleVedtak = EksternDao.selectVedtakMaksimum(
            personId = "ingenvedtak",
            fraOgMedDato = LocalDate.of(2010, 10, 1),
            tilOgMedDato = LocalDate.of(2024, 12, 31),
            h2.connection
        )

        assertThat(alleVedtak.vedtak).isEmpty()
    }

    @Test
    fun `hente ut gyldig maksimumstruktur for enkelt vedtak`() {

        val alleVedtak = EksternDao.selectVedtakMaksimum(
            personId = "123",
            fraOgMedDato = LocalDate.of(2010, 10, 1),
            tilOgMedDato = LocalDate.of(2024, 12, 31),
            h2.connection
        )



        val vedtakTime = alleVedtak.vedtak.map{vedtak ->
            LocalDateTime.parse(vedtak.vedtaksdato, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        }
        assertThat(vedtakTime).isNotEmpty
    }
}