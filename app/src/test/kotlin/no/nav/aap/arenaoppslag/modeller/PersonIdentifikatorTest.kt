package no.nav.aap.arenaoppslag.modeller

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PersonIdentifikatorTest {
    @Test
    fun `fromString oppretter personidentifikator med elleve sifre`() {
        assertThat(PersonIdentifikator.fromString("12312312312"))
            .isEqualTo(PersonIdentifikator("12312312312"))
        assertThat(PersonIdentifikator.fromString("00000000007"))
            .isEqualTo(PersonIdentifikator("00000000007"))
    }

    @Test
    fun `fromString returnerer null for ugyldig personidentifikator`() {
        listOf(null, "", "1231231231", "123123123123", "1231231231a", "1231231231 ", " 12312312312")
            .forEach { id ->
                assertThat(PersonIdentifikator.fromString(id)).isNull()
            }
    }
}
