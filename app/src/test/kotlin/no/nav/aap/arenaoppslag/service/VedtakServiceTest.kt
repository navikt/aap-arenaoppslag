package no.nav.aap.arenaoppslag.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.arenaoppslag.database.VedtakRepository
import no.nav.aap.arenaoppslag.database.VedtakfaktaRepository
import no.nav.aap.arenaoppslag.modeller.ArenaVedtakMedMaxdato
import no.nav.aap.arenaoppslag.modeller.ArenaVedtakfakta
import no.nav.aap.arenaoppslag.modeller.PersonId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VedtakServiceTest {
    private val vedtakRepository = mockk<VedtakRepository>()
    private val vedtakfaktaRepository = mockk<VedtakfaktaRepository>()
    private val service = VedtakService(vedtakRepository, vedtakfaktaRepository)
    private val personId = PersonId(42)
    private val unntaksdato = LocalDate.of(2026, 2, 1)
    private val vedtak = ArenaVedtakMedMaxdato(
        sakId = 1,
        vedtakId = 10,
        opprettetAar = 2025,
        lopenrVedtak = 1,
        lopenrSak = 7,
        statusKode = "IVERK",
        vedtaktypeKode = "O",
        aktivitetsfaseKode = "UA",
        fraOgMed = LocalDate.of(2025, 2, 1),
        tilDato = unntaksdato.minusDays(1),
        rettighetkode = "AAP",
        utfallkode = "JA",
        maxdatoUnntak = null,
        maxdatoOrdinaer = unntaksdato.minusDays(1),
    )

    @Test
    fun `beholder vurderingen og fjerner unntaksdato ved avslag`() {
        every { vedtakRepository.hentSisteLøpendeVedtakDetalj(personId) } returns vedtak

        for ((verdi, innvilget) in listOf("J" to true, "N" to false)) {
            for (dato in listOf(null, "", "01-02-2026")) {
                every { vedtakfaktaRepository.hentForVedtakIder(listOf(10)) } returns mapOf(
                    10 to buildList {
                        add(ArenaVedtakfakta("UNNTAKAAP", "Unntak", verdi, unntaksdato))
                        if (dato != null) {
                            add(ArenaVedtakfakta("AAPVILKUNN", "Unntaksdato", dato, unntaksdato))
                        }
                    }
                )

                val resultat = requireNotNull(service.hentSisteLøpendeVedtakDetalj(personId))

                assertThat(resultat.unntaksvilkaarInnvilget).isEqualTo(innvilget)
                assertThat(resultat.unntaksvilkaarGjelderFra)
                    .isEqualTo(if (innvilget && !dato.isNullOrBlank()) unntaksdato else null)
                assertThat(resultat.toKontrakt().maxdatoAap).isEqualTo(vedtak.maxdatoOrdinaer)
            }
        }
    }

    @Test
    fun `manglende fakta beholder vedtaket uten unntaksstatus`() {
        every { vedtakRepository.hentSisteLøpendeVedtakDetalj(personId) } returns vedtak
        every { vedtakfaktaRepository.hentForVedtakIder(listOf(10)) } returns emptyMap()

        assertThat(service.hentSisteLøpendeVedtakDetalj(personId)).isEqualTo(vedtak)
    }

    @Test
    fun `manglende vedtak gir null uten å hente fakta`() {
        every { vedtakRepository.hentSisteLøpendeVedtakDetalj(personId) } returns null

        assertThat(service.hentSisteLøpendeVedtakDetalj(personId)).isNull()

        verify(exactly = 0) { vedtakfaktaRepository.hentForVedtakIder(any()) }
    }
}
