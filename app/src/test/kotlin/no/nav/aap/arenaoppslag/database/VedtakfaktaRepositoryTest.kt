package no.nav.aap.arenaoppslag.database

import no.nav.aap.arenaoppslag.modeller.ArenaVedtakfakta
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VedtakfaktaRepositoryTest : H2TestBase("flyway/vedtakfakta") {

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

    @Test
    fun `somBooleanVerdi og somDatoVerdi tolker vedtakfakta hentet fra databasen`() {
        // VEDTAK_ID 1234 har UNNTAKAAP='J' og AAPVILKUNN='01-02-2025' fra V1_14__insert_vedtakfakta.sql
        val fakta = repo.hentForVedtakIder(listOf(1234))[1234]

        val unntak = fakta?.first { it.kode == "UNNTAKAAP" }
        val unntaksdato = fakta?.first { it.kode == "AAPVILKUNN" }

        assertThat(unntak?.somBooleanVerdi()).isTrue()
        assertThat(unntaksdato?.somDatoVerdi()).isEqualTo(LocalDate.of(2025, 2, 1))
    }

    @Test
    fun `somBooleanVerdi tolker N som false fra databasen`() {
        // VEDTAK_ID 4321 har UNNTAKAAP='N' fra V1_14__insert_vedtakfakta.sql
        val fakta = repo.hentForVedtakIder(listOf(4321))[4321]

        assertThat(fakta?.first { it.kode == "UNNTAKAAP" }?.somBooleanVerdi()).isFalse()
    }
}
