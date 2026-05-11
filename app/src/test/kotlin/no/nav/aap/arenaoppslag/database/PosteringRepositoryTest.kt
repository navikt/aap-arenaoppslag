package no.nav.aap.arenaoppslag.database

import no.nav.aap.arenaoppslag.modeller.PersonId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PosteringRepositoryTest : H2TestBase("flyway/postering") {

    private lateinit var posteringRepository: PosteringRepository

    @BeforeEach
    fun setUp() {
        posteringRepository = PosteringRepository(h2)
    }

    @Test
    fun `finner ingen på ukjent sak_id`() {
        val funnet = posteringRepository.hentSisteAapUtbetalingForPerson(PersonId(0xdeadbeef.toInt()))
        assertThat(funnet).isNull()
    }

    @Test
    fun `finner nyeste postering på kjent sak_id`() {
        val funnet = posteringRepository.hentSisteAapUtbetalingForPerson(PersonId(1))
        assertThat(funnet).isNotNull
        assertThat(funnet).isEqualTo(LocalDate.of(2023, 9, 12))
    }

}