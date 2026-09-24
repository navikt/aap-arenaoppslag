package no.nav.aap.arenaoppslag.database

import no.nav.aap.arenaoppslag.modeller.SakId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class Gjeldende115VedtakRepositoryTest : H2TestBase("flyway/migrering") {

    private val repo = VedtakRepository(h2)
    private val idag = LocalDate.now()

    @Test
    fun `velger siste iverksatte og innvilgede 11-5-vedtak som dekker dagens dato`() {
        val vedtak = repo.hentGjeldende115VedtakForSak(SakId(9104), idag)

        assertThat(vedtak).isNotNull
        assertThat(vedtak!!.vedtakId).isEqualTo(91045)
        assertThat(vedtak.rettighetkode).isEqualTo("AA115")
        assertThat(vedtak.begrunnelse).isEqualTo("Arbeidsevnen er nedsatt med minst halvparten")
    }

    @Test
    fun `velger fremtidig vedtak når det har blitt gjeldende`() {
        val vedtak = repo.hentGjeldende115VedtakForSak(SakId(9104), idag.plusDays(40))

        assertThat(vedtak?.vedtakId).isEqualTo(91046)
    }

    @Test
    fun `returnerer null når saken ikke har noe 11-5-vedtak`() {
        assertThat(repo.hentGjeldende115VedtakForSak(SakId(9101), idag)).isNull()
    }

    @Test
    fun `returnerer null når ingen 11-5-vedtak dekker datoen`() {
        assertThat(repo.hentGjeldende115VedtakForSak(SakId(9104), idag.minusDays(1000))).isNull()
    }
}
