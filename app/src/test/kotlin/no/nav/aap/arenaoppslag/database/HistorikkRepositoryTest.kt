package no.nav.aap.arenaoppslag.database

import no.nav.aap.arenaoppslag.modeller.ArenaVedtak
import no.nav.aap.arenaoppslag.modeller.ArenaPersonId
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple.tuple
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class HistorikkRepositoryTest : H2TestBase("flyway/eksisterer") {

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
            ArenaPersonId(54601), /* finnes ikke */
            testDato,
        )
        assertThat(alleVedtak).isEmpty()
    }

    @Test
    fun `ingen signifikante vedtak for person med kun veldig gamle vedtak`() {
        val testPerson = "kun_gamle"
        val testPersonId = ArenaPersonId(992)
        val alleVedtak: List<ArenaVedtak> = vedtakRepository.hentVedtak(testPerson)
        assertThat(alleVedtak).hasSize(2)

        val signifikanteVedtak = historikkRepository.hentAlleSignifikanteVedtakForPerson(testPersonId, testDato)
        assertThat(signifikanteVedtak).isEmpty()
    }

    @Test
    fun `finner signifikante vedtak for person med kun nye vedtak`() {
        val testPersonFnr = "00000000996"
        val testPersonId = ArenaPersonId(996)
        val alleVedtak = vedtakRepository.hentVedtak(testPersonFnr)
        assertThat(alleVedtak).hasSize(3) // antall i vedtak-tabellen

        val signifikanteVedtak = historikkRepository.hentAlleSignifikanteVedtakForPerson(testPersonId, testDato)
        assertThat(signifikanteVedtak.map { it.rettighetkode }.sorted()).isEqualTo(
            listOf(
                "AAP"
            )
        )
        assertThat(signifikanteVedtak).allMatch { it.aktivitetsfaseKode == "IKKE" }
        assertThat(signifikanteVedtak).extracting("rettighetkode", "aar", "lopenrvedtak")
            .containsExactlyInAnyOrder(
                tuple("AAP", 2021, 30),
            )
    }

    @Test
    fun `finner signifikante KLAG1 KLAG2 og ANKE vedtak`() {
        val testPersonFnr = "klager_anke"
        val testPersonId = ArenaPersonId(998)
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
        val testPersonId = ArenaPersonId(999)
        // Sjekk først at vedtakene finnes i databasen og har forventede verdier
        assertThat(vedtakRepository.hentVedtak(testPersonId).map { it.rettighetkode })
            .containsExactlyInAnyOrder("KLAG1", "KLAG2", "ANKE")
        // Sjekk at ingen av dem regnes som signifikante
        assertThat(historikkRepository.hentAlleSignifikanteVedtakForPerson(testPersonId, testDato)).isEmpty()
    }

    @Test
    fun `finner ikke KLAG1 KLAG2 og ANKE med utfall TRUKK`() {
        val testPersonId = ArenaPersonId(1000)
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
        val testPersonId = ArenaPersonId(1001)
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
        val testPersonId = ArenaPersonId(997)
        val alleVedtak = vedtakRepository.hentVedtak(testPersonFnr)
        assertThat(alleVedtak).hasSize(6)

        val signifikanteVedtak = historikkRepository.hentAlleSignifikanteVedtakForPerson(testPersonId, testDato)
        assertThat(signifikanteVedtak).hasSize(1)
    }

    @Test
    fun `Ingen signifikante vedtak for person med kun avslag på AA115`() {
        val testPersonFnr = "426282"
        val testPersonId = ArenaPersonId(426282)
        val alleVedtak = vedtakRepository.hentVedtak(testPersonFnr)
        assertThat(alleVedtak).hasSize(1)

        val signifikanteVedtak = historikkRepository.hentAlleSignifikanteVedtakForPerson(testPersonId, testDato)
        assertThat(signifikanteVedtak).hasSize(0)
    }

}
