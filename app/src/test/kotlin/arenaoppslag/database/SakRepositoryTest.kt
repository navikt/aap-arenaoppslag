package arenaoppslag.database

import no.nav.aap.arenaoppslag.kontrakt.intern.SakStatus
import no.nav.aap.arenaoppslag.kontrakt.intern.Status
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import no.nav.aap.arenaoppslag.kontrakt.modeller.Periode as KontraktPeriode

class SakRepositoryTest : H2TestBase("flyway/minimumtest") {

    @Test
    fun `hente aktfasePerioder`() {
        val forventetVedtaksperioder = listOf(
            SakStatus(
                sakId = "0", Status.IVERK,
                KontraktPeriode(
                    LocalDate.of(2022, 8, 30),
                    null
                )
            )
        )
        val sakRepository = SakRepository(h2)
        val alleVedtak = sakRepository.hentSakStatuser(
            personidentifikator = "nulltildato",
        )

        assertEquals(forventetVedtaksperioder, alleVedtak)
    }
}
