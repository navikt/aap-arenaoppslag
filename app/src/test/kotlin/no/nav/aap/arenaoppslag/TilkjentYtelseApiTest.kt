package no.nav.aap.arenaoppslag

import no.nav.aap.arenaoppslag.client.ArenaOppslagGateway.Companion.withTestServer
import no.nav.aap.arenaoppslag.database.H2TestBase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

// Tilkjent ytelse eksponeres som en del av /api/intern/sak/{sakid}/detaljert-responsen,
// så vi verifiserer payloaden gjennom det endepunktet.
class TilkjentYtelseApiTest : H2TestBase("flyway/maksimum") {

    @Test
    fun `detaljert-responsen inkluderer tilkjent ytelse med meldekort for sak`() {
        withTestServer(h2) { gateway ->
            val response = gateway.hentSakDetaljert(9001)

            val tilkjentYtelse = response.tilkjentYtelse
            assertThat(tilkjentYtelse).isNotNull
            assertThat(tilkjentYtelse!!.sakId).isEqualTo(9001)
            assertThat(tilkjentYtelse.rader).hasSize(2)

            val rad = tilkjentYtelse.rader.first { it.meldekort?.meldekortId == 5001L }
            assertThat(rad.kilde).isEqualTo("Meldekort")
            assertThat(rad.uke).isEqualTo("1-2")
            assertThat(rad.beregnetBrutto).isEqualTo(7700)
            assertThat(rad.fraOgMedDato).isEqualTo(LocalDate.of(2023, 1, 2))
            assertThat(rad.meldekort?.uker).isNotEmpty()

            // 5 timer arbeidet av 14 dager à 7,5 timer = 5 %. Ingen samordning (DAGS == DAGSFSAM).
            assertThat(rad.reduksjon?.levertForSentDager).isEqualTo(0)
            assertThat(rad.reduksjon?.timerArbeidetProsent).isEqualTo(5)
            assertThat(rad.reduksjon?.samordningsProsent).isEqualTo(0)
            assertThat(rad.reduksjon?.totalReduksjonProsent).isEqualTo(5)
        }
    }
}

