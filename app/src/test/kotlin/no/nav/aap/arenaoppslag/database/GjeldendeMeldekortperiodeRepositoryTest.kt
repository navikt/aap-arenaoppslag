package no.nav.aap.arenaoppslag.database

import no.nav.aap.arenaoppslag.modeller.Periode
import no.nav.aap.arenaoppslag.modeller.PersonId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate

class GjeldendeMeldekortperiodeRepositoryTest : H2TestBase("flyway/meldeperiode") {

    private val repository = MeldekortperiodeRepository(h2)

    // Dekkes både av perioden uke 33-34 og uke 34-35
    private val onsdagUke34 = LocalDate.of(2026, 8, 19)

    @Test
    fun `bruker i oddetallssyklus får perioden som starter i uke 33`() {
        val periode = repository.hentGjeldendePeriodeForPerson(PersonId(400), onsdagUke34)

        assertThat(periode).isEqualTo(Periode(LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 23)))
        assertThat(periode?.fraOgMedDato?.dayOfWeek).isEqualTo(DayOfWeek.MONDAY)
    }

    @Test
    fun `bruker i partallssyklus får perioden som starter i uke 34`() {
        val periode = repository.hentGjeldendePeriodeForPerson(PersonId(401), onsdagUke34)

        assertThat(periode).isEqualTo(Periode(LocalDate.of(2026, 8, 17), LocalDate.of(2026, 8, 30)))
        assertThat(periode?.fraOgMedDato?.dayOfWeek).isEqualTo(DayOfWeek.MONDAY)
    }

    @Test
    fun `gir null når brukeren ikke har meldekort`() {
        assertThat(repository.hentGjeldendePeriodeForPerson(PersonId(402), onsdagUke34)).isNull()
    }

    @Test
    fun `gir null når ingen av brukerens meldekort dekker datoen`() {
        // Uke 36 dekkes kun av perioden uke 35-36, som oddetallsbrukeren ikke har meldekort for
        val onsdagUke36 = LocalDate.of(2026, 9, 2)

        assertThat(repository.hentGjeldendePeriodeForPerson(PersonId(400), onsdagUke36)).isNull()
    }
}
