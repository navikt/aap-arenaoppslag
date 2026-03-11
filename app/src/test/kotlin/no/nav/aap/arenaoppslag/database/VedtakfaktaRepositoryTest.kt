package no.nav.aap.arenaoppslag.database

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VedtakfaktaRepositoryTest :
    H2TestBase(
        "flyway/dsop",
        "flyway/minimumtest",
        "flyway/eksisterer",
        "flyway/vedtakfakta"
    ) {

    @Test
    fun `null når vedtaket ikke finnes på saken`() {
        val repository = VedtakfaktaRepository(h2)

        val dato = repository.`hentMaksdatoEtterUtløpAvKvoteForSak`(0xdeadbeef.toInt())
        assertThat(dato).isNull()
    }

    @Test
    fun `verdi når AAP-vedtaket finnes på saken`() {
        val repository = VedtakfaktaRepository(h2)

        val dato = repository.`hentMaksdatoEtterUtløpAvKvoteForSak`(111)
        assertThat(dato).isEqualTo(LocalDate.of(2021, 10, 18))
    }

    @Test
    fun `verdi når AAP-vedtaket finnes med forlengelse på saken`() {
        val repository = VedtakfaktaRepository(h2)

        val dato = repository.`hentMaksdatoEtterUtløpAvKvoteForSak`(444)
        assertThat(dato).isEqualTo(LocalDate.of(2023, 8, 8))
    }

}