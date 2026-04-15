package no.nav.aap.arenaoppslag.database

import no.nav.aap.arenaoppslag.modeller.ArenaVedtakfakta
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VedtakfaktaRepositoryTest : H2TestBase("flyway/minimumtest") {

    private val repo = VedtakfaktaRepository(h2)

    @Test
    fun `henter vedtakfakta for kjent vedtakId`() {
        // VEDTAK_ID 37067849 har én DAGS-fakta fra V1_3__arena_data.sql
        val resultat = repo.hentForVedtakIder(listOf(37067849))

        assertThat(resultat).containsKey(37067849)
        assertThat(resultat[37067849]).containsExactly(
            ArenaVedtakfakta(
                kode = "DAGS",
                navn = "Dagsats",
                verdi = "255",
                registrertDato = LocalDate.of(2025, 3, 28),
            )
        )
    }

    @Test
    fun `returnerer tom map for ukjent vedtakId`() {
        val resultat = repo.hentForVedtakIder(listOf(999999999))

        assertThat(resultat).isEmpty()
    }

    @Test
    fun `returnerer tom map for tom liste`() {
        val resultat = repo.hentForVedtakIder(emptyList())

        assertThat(resultat).isEmpty()
    }
}
