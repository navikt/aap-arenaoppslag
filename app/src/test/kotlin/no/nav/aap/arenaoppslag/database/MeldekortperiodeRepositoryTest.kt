package no.nav.aap.arenaoppslag.database

import no.nav.aap.arenaoppslag.modeller.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class MeldekortperiodeRepositoryTest : H2TestBase("flyway/maksimum") {

    @Test
    fun `hentGjeldendePeriode returnerer perioden som dekker den oppgitte datoen`() {
        val meldekortperiodeRepository = MeldekortperiodeRepository(h2)

        val periode = meldekortperiodeRepository.hentGjeldendePeriode(LocalDate.of(2023, 1, 10))

        assertThat(periode).isEqualTo(
            Periode(
                fraOgMedDato = LocalDate.of(2023, 1, 2),
                tilOgMedDato = LocalDate.of(2023, 1, 15),
            )
        )
    }

    @Test
    fun `hentGjeldendePeriode returnerer null når ingen periode dekker datoen`() {
        val meldekortperiodeRepository = MeldekortperiodeRepository(h2)

        val periode = meldekortperiodeRepository.hentGjeldendePeriode(LocalDate.of(1999, 1, 1))

        assertThat(periode).isNull()
    }
}
