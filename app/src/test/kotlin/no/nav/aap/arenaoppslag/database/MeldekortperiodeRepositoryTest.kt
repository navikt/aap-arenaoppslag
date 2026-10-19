package no.nav.aap.arenaoppslag.database

import no.nav.aap.arenaoppslag.modeller.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class MeldekortperiodeRepositoryTest : H2TestBase("flyway/maksimum") {

    @Test
    fun `hentPerioderForMeldekort returnerer meldeperioden hvert meldekort gjelder`() {
        val meldekortperiodeRepository = MeldekortperiodeRepository(h2)

        val perioder = meldekortperiodeRepository.hentPerioderForMeldekort(listOf(6501L, 999_999L))

        assertThat(perioder).containsExactlyEntriesOf(
            mapOf(6501L to Periode(LocalDate.of(2022, 12, 26), LocalDate.of(2023, 1, 8)))
        )
    }

    @Test
    fun `hentPerioderForMeldekort gir tomt resultat for tom liste`() {
        assertThat(MeldekortperiodeRepository(h2).hentPerioderForMeldekort(emptyList())).isEmpty()
    }
}
