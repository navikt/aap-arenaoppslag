package no.nav.aap.arenaoppslag.database

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import no.nav.aap.arenaoppslag.Metrics
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DbDispatcherMetricsTest {

    @Test
    fun `maler ko-tid og teller aktive kall for et vellykket db-kall`() = runBlocking {
        val aktiveForVentende = Metrics.dbDispatcherAktiveKall.get()

        val svar = Dispatchers.Unconfined.målDbKall("test-vellykket") { "resultat" }

        assertEquals("resultat", svar)
        assertEquals(aktiveForVentende, Metrics.dbDispatcherAktiveKall.get())
        assertEquals(0, Metrics.dbDispatcherVentendeKall.get())

        val timer = Metrics.prometheus.find("arenaoppslag_db_dispatcher_ko_tid_seconds")
            .tag("kall", "test-vellykket")
            .timer()
        assertTrue(timer != null && timer.count() >= 1)
    }

    @Test
    fun `teller ned ventende og aktive kall selv om blokken kaster`() = runBlocking {
        val forrigeAktive = Metrics.dbDispatcherAktiveKall.get()
        val forrigeVentende = Metrics.dbDispatcherVentendeKall.get()

        try {
            Dispatchers.Unconfined.målDbKall<Unit>("test-feilende") { error("boom") }
        } catch (e: IllegalStateException) {
            // forventet
        }

        assertEquals(forrigeAktive, Metrics.dbDispatcherAktiveKall.get())
        assertEquals(forrigeVentende, Metrics.dbDispatcherVentendeKall.get())
    }
}
