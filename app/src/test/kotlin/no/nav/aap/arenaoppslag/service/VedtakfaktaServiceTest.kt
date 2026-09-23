package no.nav.aap.arenaoppslag.service

import io.mockk.every
import io.mockk.mockk
import no.nav.aap.arenaoppslag.database.VedtakfaktaRepository
import no.nav.aap.arenaoppslag.modeller.ArenaVedtakfakta
import no.nav.aap.arenaoppslag.modeller.VedtakId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VedtakfaktaServiceTest {

    private val repo = mockk<VedtakfaktaRepository>()
    private val service = VedtakfaktaService(repo)

    @Test
    fun `hentVedtakfaktaForVedtak returnerer faktaene fra repository`() {
        val fakta = ArenaVedtakfakta(
            kode = "DAGS",
            navn = "Dagsats",
            verdi = "255",
            registrertDato = LocalDate.of(2025, 3, 28),
        )
        every { repo.hentForVedtakId(VedtakId(1234)) } returns listOf(fakta)

        assertThat(service.hentVedtakfaktaForVedtak(VedtakId(1234))).containsExactly(fakta)
    }

    @Test
    fun `hentVedtakfaktaForVedtak returnerer tom liste for vedtak uten fakta`() {
        every { repo.hentForVedtakId(VedtakId(999)) } returns emptyList()

        assertThat(service.hentVedtakfaktaForVedtak(VedtakId(999))).isEmpty()
    }
}

