package no.nav.aap.arenaoppslag.database

import no.nav.aap.arenaoppslag.modeller.ArenaVedtak
import no.nav.aap.arenaoppslag.modeller.PersonId
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple.tuple
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class HistorikkRepositoryTest : H2TestBase("flyway/eksisterer", "flyway/historikkaktivitetsfase") {

    private lateinit var historikkRepository: HistorikkRepository
    private lateinit var vedtakRepository: VedtakRepository
    private val testDato = LocalDate.parse("2025-12-15") // simulerer dagens dato for testene

    @BeforeEach
    fun setUp() {
        historikkRepository = HistorikkRepository(h2)
        vedtakRepository = VedtakRepository(h2)
    }

    @Test
    fun `ingen signifikante vedtak for person som ikke finnes`() {
        val alleVedtak = historikkRepository.hentAlleSignifikanteVedtakForPerson(
            PersonId(54601), /* finnes ikke */
            testDato,
        )
        assertThat(alleVedtak).isEmpty()
    }

    @Test
    fun `ingen signifikante vedtak for person med kun veldig gamle vedtak`() {
        val testPerson = "kun_gamle"
        val testPersonId = PersonId(992)
        val alleVedtak: List<ArenaVedtak> = vedtakRepository.hentVedtak(testPerson)
        assertThat(alleVedtak).hasSize(2)

        val signifikanteVedtak = historikkRepository.hentAlleSignifikanteVedtakForPerson(testPersonId, testDato)
        assertThat(signifikanteVedtak).isEmpty()
    }

    @Test
    fun `finner signifikante vedtak for person med kun nye vedtak`() {
        val testPersonFnr = "kun_nye"
        val testPersonId = PersonId(996)
        val alleVedtak = vedtakRepository.hentVedtak(testPersonFnr)
        assertThat(alleVedtak).hasSize(3) // antall i vedtak-tabellen

        val signifikanteVedtak = historikkRepository.hentAlleSignifikanteVedtakForPerson(testPersonId, testDato)
        assertThat(signifikanteVedtak.map { it.rettighetkode }.sorted()).isEqualTo(
            listOf(
                "AAP"
            )
        )
        assertThat(signifikanteVedtak).allMatch { it.aktivitetsfaseKode == "UA" }
        assertThat(signifikanteVedtak).extracting("rettighetkode", "aar", "lopenrvedtak")
            .containsExactlyInAnyOrder(
                tuple("AAP", 2021, 30),
            )
    }

    @Test
    fun `finner signifikante KLAG1 KLAG2 og ANKE vedtak`() {
        val testPersonFnr = "klager_anke"
        val testPersonId = PersonId(998)
        // Sjekk først at vedtakene finnes i databasen og har forventede verdier
        val alleVedtak = vedtakRepository.hentVedtak(testPersonFnr)
        assertThat(alleVedtak).hasSize(3)

        // Sjekk deretter at de regnes som signifikante
        val signifikanteVedtak = historikkRepository.hentAlleSignifikanteVedtakForPerson(testPersonId, testDato)
        assertThat(signifikanteVedtak.map { it.rettighetkode })
            .containsExactlyInAnyOrder("KLAG1", "KLAG2", "ANKE")
    }

    @Test
    fun `finner ikke KLAG1 KLAG2 og ANKE eldre enn maksimal behandlingstid`() {
        val testPersonId = PersonId(999)
        // Sjekk først at vedtakene finnes i databasen og har forventede verdier
        assertThat(vedtakRepository.hentVedtak(testPersonId).map { it.rettighetkode })
            .containsExactlyInAnyOrder("KLAG1", "KLAG2", "ANKE")
        // Sjekk at ingen av dem regnes som signifikante
        assertThat(historikkRepository.hentAlleSignifikanteVedtakForPerson(testPersonId, testDato)).isEmpty()
    }

    @Test
    fun `finner ikke KLAG1 KLAG2 og ANKE med utfall TRUKK`() {
        val testPersonId = PersonId(1000)
        val alleVedtak = vedtakRepository.hentVedtak(testPersonId)
        // Sjekk først at vedtakene finnes i databasen og har forventede verdier
        assertThat(alleVedtak.map { it.rettighetkode })
            .containsExactlyInAnyOrder("KLAG1", "KLAG2", "ANKE")
        assertThat(alleVedtak).allMatch { it.utfallkode == "TRUKK" }

        // Sjekk at ingen av dem regnes som signifikante
        assertThat(historikkRepository.hentAlleSignifikanteVedtakForPerson(testPersonId, testDato)).isEmpty()
    }

    @Test
    fun `finner ikke klager behandlet for mer enn 12 uker siden eller anker behandlet for mer enn 52 uker siden`() {
        val testPersonId = PersonId(1001)
        val alleVedtak = vedtakRepository.hentVedtak(testPersonId)
        // Sjekk først at vedtakene finnes i databasen og har forventede verdier
        assertThat(alleVedtak.map { it.rettighetkode })
            .containsExactlyInAnyOrder("KLAG1", "KLAG2", "ANKE")
        assertThat(alleVedtak).allMatch { it.utfallkode == "NEI" }

        // Sjekk at ingen av dem regnes som signifikante
        assertThat(historikkRepository.hentAlleSignifikanteVedtakForPerson(testPersonId, testDato)).isEmpty()
    }

    @Test
    fun `finner signifikante vedtak for person med både gamle og nye vedtak`() {
        val testPersonFnr = "blanding"
        val testPersonId = PersonId(997)
        val alleVedtak = vedtakRepository.hentVedtak(testPersonFnr)
        assertThat(alleVedtak).hasSize(6)

        val signifikanteVedtak = historikkRepository.hentAlleSignifikanteVedtakForPerson(testPersonId, testDato)
        assertThat(signifikanteVedtak).extracting("rettighetkode", "vedtaktypeKode", "aktivitetsfaseKode")
            .containsExactly(tuple("AAP", "G", "AU"))
    }

    @Test
    fun `finner bare vanlige AAP-vedtak i UA og AU også når vedtakene er under behandling`() {
        val testPersonId = PersonId(21001)
        val alleVedtak = vedtakRepository.hentVedtak(testPersonId)
        assertThat(alleVedtak).hasSize(14)
        assertThat(alleVedtak.map { it.aktivitetsfaseKode })
            .contains("UA", "AU", "IKKE", "SPE", "UVUP", "FA")

        val signifikanteVedtak = historikkRepository.hentAlleSignifikanteVedtakForPerson(testPersonId, testDato)

        assertThat(signifikanteVedtak).extracting("vedtaktypeKode", "aktivitetsfaseKode", "statusKode", "utfallkode")
            .containsExactlyInAnyOrder(
                tuple("O", "UA", "IVERK", "JA"),
                tuple("O", "AU", "IVERK", "JA"),
                tuple("E", "UA", "IVERK", "JA"),
                tuple("E", "AU", "IVERK", "JA"),
                tuple("G", "UA", "IVERK", "JA"),
                tuple("G", "AU", "IVERK", "JA"),
                tuple("O", "UA", "REGIS", null),
                tuple("E", "AU", "REGIS", null)
            )
        assertThat(signifikanteVedtak.map { it.lopenrvedtak }).containsExactlyInAnyOrder(14, 13, 10, 9, 6, 5, 2, 1)
    }

    @Test
    fun `ingen signifikante vedtak for person med bare vanlige AAP-vedtak utenfor UA og AU`() {
        val testPersonId = PersonId(21003)
        assertThat(vedtakRepository.hentVedtak(testPersonId))
            .extracting("vedtaktypeKode", "aktivitetsfaseKode")
            .containsExactlyInAnyOrder(tuple("O", "IKKE"), tuple("E", "SPE"), tuple("G", "UVUP"))

        assertThat(historikkRepository.hentAlleSignifikanteVedtakForPerson(testPersonId, testDato)).isEmpty()
    }

    @Test
    fun `finner relevante stanser med og uten sluttdato også utenfor UA og AU`() {
        val testPersonId = PersonId(21002)
        assertThat(vedtakRepository.hentVedtak(testPersonId)).hasSize(8)

        val signifikanteVedtak = historikkRepository.hentAlleSignifikanteVedtakForPerson(testPersonId, testDato)

        assertThat(signifikanteVedtak).allMatch { it.vedtaktypeKode == "S" }
        assertThat(signifikanteVedtak).extracting("aktivitetsfaseKode", "tilDato")
            .containsExactlyInAnyOrder(
                tuple("UA", LocalDate.of(2025, 12, 31)),
                tuple("AU", LocalDate.of(2025, 12, 31)),
                tuple("IKKE", LocalDate.of(2025, 12, 31)),
                tuple("SPE", LocalDate.of(2025, 12, 31)),
                tuple("UA", null),
                tuple("AU", null),
                tuple("IKKE", null),
                tuple("SPE", null)
            )
    }

    @Test
    fun `finner ubehandlet AA115 selv om aktivitetsfasen ikke er UA eller AU`() {
        val signifikanteVedtak = historikkRepository.hentAlleSignifikanteVedtakForPerson(PersonId(21004), testDato)

        assertThat(signifikanteVedtak).extracting("rettighetkode", "aktivitetsfaseKode", "utfallkode")
            .containsExactly(tuple("AA115", "IKKE", null))
    }

    @Test
    fun `Ingen signifikante vedtak for person med kun avslag på AA115`() {
        val testPersonFnr = "426282"
        val testPersonId = PersonId(426282)
        val alleVedtak = vedtakRepository.hentVedtak(testPersonFnr)
        assertThat(alleVedtak).hasSize(1)

        val signifikanteVedtak = historikkRepository.hentAlleSignifikanteVedtakForPerson(testPersonId, testDato)
        assertThat(signifikanteVedtak).hasSize(0)
    }

}
