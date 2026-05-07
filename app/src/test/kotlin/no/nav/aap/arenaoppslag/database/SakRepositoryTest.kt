package no.nav.aap.arenaoppslag.database

import no.nav.aap.arenaoppslag.modeller.ArenaSak
import no.nav.aap.arenaoppslag.modeller.ArenaSakOppsummering
import no.nav.aap.arenaoppslag.modeller.ArenaSakPerson
import no.nav.aap.arenaoppslag.modeller.Maksdatolinje
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SakRepositoryTest : H2TestBase("flyway/minimumtest", "flyway/saklistetest") {

    private lateinit var sakRepository: SakRepository

    @BeforeEach
    fun setup() {
        sakRepository = SakRepository(h2)
    }

    @Test
    fun `klarer å hente en sak fra databasen`() {
        val forventetSak = ArenaSak(
            sakId = "1",
            opprettetAar = 2021,
            lopenr = 1,
            statuskode = "AKTIV",
            statusnavn = "Aktiv",
            registrertDato = LocalDate.of(2022, 2, 1).atStartOfDay(),
            avsluttetDato = null,
            person = ArenaSakPerson(
                personId = 1,
                fodselsnummer = "123",
                fornavn = "Test",
                etternavn = "Bestesen",
            )
        )
        val sak = sakRepository.hentSak(1)
        assertThat(sak).isEqualTo(forventetSak)
    }

    @Test
    fun `returnerer NULL om saken ikke finnes i databasen`() {
        val sak = sakRepository.hentSak(1919191919)
        assertThat(sak).isNull()
    }

    @Test
    fun `hentSakerForPersnNummere returnerer saker for en kjent person med én sak og ett vedtak`() {
        val forventetSak = ArenaSakOppsummering(
            sakId = "1",
            lopenummer = 1,
            aar = 2021,
            antallVedtak = 1,
            sakstype = "Arbeidsavklaringspenger",
            statuskode = "AKTIV",
            statusnavn = "Aktiv",
            regDato = LocalDate.of(2022, 2, 1),
            avsluttetDato = null,
        )
        val saker = sakRepository.hentSakerForPerson(setOf("123"))
        assertThat(saker).containsExactly(forventetSak)
    }

    @Test
    fun `hentSakerForPersnNummere returnerer tom liste for ukjent person`() {
        val saker = sakRepository.hentSakerForPerson(setOf("007"))
        assertThat(saker).isEmpty()
    }

    @Test
    fun `hentSakerForPersnNummere returnerer tom liste for person uten saker`() {
        // Person "ingenvedtak" (person_id=3) er registrert uten noen saker
        val saker = sakRepository.hentSakerForPerson(setOf("ingenvedtak"))
        assertThat(saker).isEmpty()
    }

    @Test
    fun `hentSakerForPersnNummere henter alle saker og teller vedtak per sak korrekt`() {
        val forventedeSaker = listOf(
            ArenaSakOppsummering(
                sakId = "901",
                lopenummer = 1,
                aar = 2020,
                antallVedtak = 2,
                sakstype = "Arbeidsavklaringspenger",
                statuskode = "INAKT",
                statusnavn = "Inaktiv",
                regDato = LocalDate.of(2020, 1, 15),
                avsluttetDato = null,
            ),
            ArenaSakOppsummering(
                sakId = "902",
                lopenummer = 2,
                aar = 2023,
                antallVedtak = 1,
                sakstype = "Arbeidsavklaringspenger",
                statuskode = "INAKT",
                statusnavn = "Inaktiv",
                regDato = LocalDate.of(2023, 3, 1),
                avsluttetDato = LocalDate.of(2023, 12, 31),
            ),
        )
        val saker = sakRepository.hentSakerForPerson(setOf("tosaker"))
        assertThat(saker).containsExactlyInAnyOrderElementsOf(forventedeSaker)
    }

    @Test
    fun `hentSakerForPersnNummere returnerer saker for flere personer i ett kall`() {
        val saker = sakRepository.hentSakerForPerson(setOf("123", "tosaker"))
        // "123" har 1 sak, "tosaker" har 2 saker - totalt 3 saker
        assertThat(saker).hasSize(3)
        assertThat(saker.map { it.sakId }).containsExactlyInAnyOrder("1", "901", "902")
    }

    @Test
    fun `hentSakerForPersnNummere returnerer tom liste naar settet er tomt`() {
        val saker = sakRepository.hentSakerForPerson(emptySet())
        assertThat(saker).isEmpty()
    }

    @Test
    fun `hent maksdato paa saker finner forventede data`() {
        val forventet = listOf(
            Maksdatolinje(
                2, 1335, "IKKE", "O",
                LocalDate.of(2010, 8, 29),
                LocalDate.of(2026, 6, 30),
                LocalDate.of(2025, 6, 30),
                sakRegistrert = LocalDate.of(2022,2,2),
                sakAvsluttet = null,
                sakStatus = "AKTIV"
            ),
            Maksdatolinje(
                3, 30, "IKKE", "O",
                LocalDate.of(2022, 8, 30),
                LocalDate.of(2025, 12, 31),
                LocalDate.of(2025, 6, 30),
                sakRegistrert = LocalDate.of(2022,2,3),
                sakAvsluttet = null,
                sakStatus = "AKTIV"
            )
        )
        val saker = sakRepository.hentSakerMedMaksDatoOgVedtak(setOf(1, 2, 3))
        assertThat(saker).hasSize(2)
        assertThat(saker).containsAll(forventet) // mappingen testes
    }

    @Test
    fun `hent maksdato paa saker gir tom liste for tom liste`() {
        val saker = sakRepository.hentSakerMedMaksDatoOgVedtak(emptySet())
        assertThat(saker).isEmpty()
    }

    @Test
    fun `hent maksdato paa saker finner ikke data som mangler`() {
        val saker = sakRepository.hentSakerMedMaksDatoOgVedtak(setOf(4020))
        assertThat(saker).isEmpty()
    }
}
