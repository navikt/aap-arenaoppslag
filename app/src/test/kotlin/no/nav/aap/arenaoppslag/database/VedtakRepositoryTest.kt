package no.nav.aap.arenaoppslag.database

import no.nav.aap.arenaoppslag.kontrakt.intern.Status
import no.nav.aap.arenaoppslag.modeller.VedtakStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import no.nav.aap.arenaoppslag.kontrakt.modeller.Periode as KontraktPeriode

class VedtakRepositoryTest : H2TestBase("flyway/minimumtest") {

    @Test
    fun `hente aktfasePerioder`() {
        val forventetVedtaksperioder = listOf(
            VedtakStatus(
                sakId = "4", Status.IVERK,
                KontraktPeriode(
                    LocalDate.of(2022, 8, 30),
                    null
                )
            )
        )
        val vedtakRepository = VedtakRepository(h2)
        val alleVedtak = vedtakRepository.hentVedtakStatuser(
            fnr = "nulltildato",
        )

        assertEquals(forventetVedtaksperioder, alleVedtak)
    }
}
