package no.nav.aap.arenaoppslag.database

import no.nav.aap.arenaoppslag.modeller.MedisinskOpplysning
import no.nav.aap.arenaoppslag.modeller.PersonId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class MedisinskOpplysningRepositoryTest : H2TestBase("flyway/migrering") {

    private val repo = MedisinskOpplysningRepository(h2)

    @Test
    fun `henter alle medisinske opplysninger for personen sortert på kildedato`() {
        val opplysninger = repo.hentForPerson(PersonId(203))

        assertThat(opplysninger).containsExactly(
            MedisinskOpplysning(92011, "ICPC2", "L84", "HOVED", LocalDate.of(2023, 2, 1)),
            MedisinskOpplysning(92012, "ICD10", "M54", "BI", LocalDate.of(2023, 3, 1)),
            MedisinskOpplysning(92013, "ICPC2", "P76", "BI", LocalDate.of(2023, 4, 1)),
        )
    }

    @Test
    fun `tar ikke med opplysninger som tilhører en annen person`() {
        val opplysninger = repo.hentForPerson(PersonId(203))

        assertThat(opplysninger.map { it.medisinskOpplysningId }).doesNotContain(92021)
    }

    @Test
    fun `gir tom liste for person uten attføringsopplysning`() {
        assertThat(repo.hentForPerson(PersonId(201))).isEmpty()
    }
}
