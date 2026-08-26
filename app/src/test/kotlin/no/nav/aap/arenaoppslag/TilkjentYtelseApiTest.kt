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

            // 5 timer arbeidet av 10 arbeidsdager à 7,5 timer = 75 timer → 7 %. Ingen samordning (DAGS == DAGSFSAM). insGrad = 33 → total = 7 + 0 + 33 = 40 %.
            assertThat(rad.reduksjon?.levertForSentDager).isEqualTo(0)
            assertThat(rad.reduksjon?.timerArbeidetProsent).isEqualTo(7)
            assertThat(rad.reduksjon?.samordningsProsent).isEqualTo(0)
            assertThat(rad.reduksjon?.totalReduksjonProsent).isEqualTo(40)
            assertThat(rad.reduksjon?.fravar).isEqualTo(0.0f)
            assertThat(rad.reduksjon?.sykedager).isEqualTo(1.0f)
            assertThat(rad.reduksjon?.institusjonsProsent).isEqualTo(33)

            // Anmerkningene ligger på meldekortet, også de som ikke gir reduksjon (MAXAA).
            assertThat(rad.meldekort?.anmerkninger?.map { it.kode }).containsExactly("FSNN", "MAXAA")
            val sykdom = rad.meldekort?.anmerkninger?.first { it.kode == "FSNN" }
            assertThat(sykdom?.navn).isEqualTo("Fravær av type S")
            assertThat(sykdom?.beskrivelseFlettet).isEqualTo("Utbetalingen er redusert pga sykdom 1 dager")

            // Meldekort 5001 trekker kun ordinær kvote (20 - 10), unntakskvoten videreføres fra INIT.
            assertThat(rad.gjenstaaendeOrdinaerDager).isEqualTo(10)
            assertThat(rad.gjenstaaendeUnntakDager).isEqualTo(30)

            val radMeldekortTo = tilkjentYtelse.rader.first { it.meldekort?.meldekortId == 5002L }
            assertThat(radMeldekortTo.gjenstaaendeOrdinaerDager).isEqualTo(4)
            assertThat(radMeldekortTo.gjenstaaendeUnntakDager).isEqualTo(25)

            // Saldoen for personen som helhet kommer fra BEREGNINGSLEDD og ligger på telleverkForPerson,
            // ikke på tilkjent ytelse.
            assertThat(response.telleverkForPerson?.ordineerAAPKvote).isEqualTo(4)
            assertThat(response.telleverkForPerson?.utvidetAAPKvote).isEqualTo(25)
        }
    }
}

