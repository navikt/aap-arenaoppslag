package arenaoppslag.intern

import arenaoppslag.util.H2TestBase
import no.nav.aap.arenaoppslag.kontrakt.intern.SakStatus
import no.nav.aap.arenaoppslag.kontrakt.intern.Status
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import no.nav.aap.arenaoppslag.kontrakt.modeller.Periode as KontraktPeriode

class SakerDaoTest : H2TestBase("flyway/minimumtest") {

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

        val alleVedtak = SakerDao.selectSaker(
            personidentifikator = "nulltildato",
            h2.connection
        )

        assertEquals(forventetVedtaksperioder, alleVedtak)
    }
}
