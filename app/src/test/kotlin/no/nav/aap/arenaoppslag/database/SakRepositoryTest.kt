package no.nav.aap.arenaoppslag.database

import no.nav.aap.arenaoppslag.modeller.ArenaSak
import no.nav.aap.arenaoppslag.modeller.ArenaSakOppsummering
import no.nav.aap.arenaoppslag.modeller.ArenaSakPerson
import no.nav.aap.arenaoppslag.modeller.Maksdatolinje
import no.nav.aap.arenaoppslag.modeller.ArenaPersonId
import no.nav.aap.arenaoppslag.modeller.PersonIdentifikator
import no.nav.aap.arenaoppslag.modeller.Saksnummer
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
    fun `hentPersonIdentifikatorHvisEksisterer returnerer personidentifikator for en personsak`() {
        val personidentifikator = sakRepository.hentPersonIdentifikatorHvisEksisterer(Saksnummer(lopenummerSak = 1, aar = 2020))

        assertThat(personidentifikator).isEqualTo(PersonIdentifikator("00000000900"))
    }

    @Test
    fun `hentPersonIdentifikatorHvisEksisterer returnerer null for ukjent saksnummer`() {
        val personidentifikator = sakRepository.hentPersonIdentifikatorHvisEksisterer(Saksnummer(lopenummerSak = 99999, aar = 1919))

        assertThat(personidentifikator).isNull()
    }

    @Test
    fun `hentPersonIdentifikatorHvisEksisterer krever treff på både år og løpenummer`() {
        assertThat(
            sakRepository.hentPersonIdentifikatorHvisEksisterer(Saksnummer(lopenummerSak = 1, aar = 1919))
        ).isNull()
        assertThat(
            sakRepository.hentPersonIdentifikatorHvisEksisterer(Saksnummer(lopenummerSak = 99999, aar = 2020))
        ).isNull()
    }

    @Test
    fun `hentPersonIdentifikatorHvisEksisterer returnerer null når saken ikke gjelder PERSON`() {
        val personidentifikator = sakRepository.hentPersonIdentifikatorHvisEksisterer(Saksnummer(lopenummerSak = 3, aar = 2023))

        assertThat(personidentifikator).isNull()
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
                fodselsnummer = "00000000123",
                fornavn = "Test",
                etternavn = "Bestesen",
            )
        )
        val sak = sakRepository.hentSakUtenTilgangskontroll(Saksnummer(lopenummerSak = 1, aar = 2021))
        assertThat(sak).isEqualTo(forventetSak)
    }

    @Test
    fun `returnerer NULL om saken ikke finnes i databasen`() {
        val sak = sakRepository.hentSakUtenTilgangskontroll(Saksnummer(lopenummerSak = 1919191919, aar = 1919))
        assertThat(sak).isNull()
    }

    @Test
    fun `hentSakerForPerson returnerer saker for en kjent person med én sak og ett vedtak`() {
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
        // person_id=1 tilsvarer fnr "00000000123" i minimumtest-datasettet
        val saker = sakRepository.hentSakerForPerson(ArenaPersonId(1))
        assertThat(saker).containsExactly(forventetSak)
    }

    @Test
    fun `hentSakerForPerson returnerer tom liste for ukjent person`() {
        val saker = sakRepository.hentSakerForPerson(ArenaPersonId(99999))
        assertThat(saker).isEmpty()
    }

    @Test
    fun `hentSakerForPerson returnerer tom liste for person uten saker`() {
        // Person "00000000003" har person_id=3 i minimumtest-datasettet
        val saker = sakRepository.hentSakerForPerson(ArenaPersonId(3))
        assertThat(saker).isEmpty()
    }

    @Test
    fun `hentSakerForPerson henter alle saker og teller vedtak per sak korrekt`() {
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
        // person_id=900 tilsvarer fnr "00000000900" i saklistetest-datasettet
        val saker = sakRepository.hentSakerForPerson(ArenaPersonId(900))
        assertThat(saker).containsExactlyInAnyOrderElementsOf(forventedeSaker)
    }

    @Test
    fun `hent maksdato paa saker for person finner forventede data`() {
        val sak = sakRepository.hentMaxdatoForSisteVedtak(ArenaPersonId(102))
        assertThat(sak).isEqualTo(
            Maksdatolinje(
                1_12_0, 2022, 1_12_0, 1_12_2, "IKKE", "O",
                vedtakstatuskode = "IVERK",
                LocalDate.of(2027, 1, 1),
                LocalDate.of(2011, 1, 1),
                LocalDate.of(2027, 1, 1),
                LocalDate.of(2026, 6, 30),
                sakRegistrert = LocalDate.of(2022, 2, 2),
                sakAvsluttet = LocalDate.of(2024, 6, 15),
                sakStatus = "AVSLU",
            )
        )
    }

    @Test
    fun `hent maksdato paa saker for person med vedtak som har annen aktfasekode`() {
        val sak = sakRepository.hentMaxdatoForSisteVedtak(ArenaPersonId(103))
        assertThat(sak).isEqualTo(
            Maksdatolinje(
                1_13_0, 2022, 1_13_0, 1_13_1, "IKKE", "O",
                vedtakstatuskode = "IVERK",
                LocalDate.of(2030, 1, 1),
                LocalDate.of(2020, 1, 1),
                LocalDate.of(2031, 1, 1),
                LocalDate.of(2030, 1, 1),
                sakRegistrert = LocalDate.of(2022, 2, 10),
                sakAvsluttet = null,
                sakStatus = "AKTIV",
            )
        )
    }

    @Test
    fun `hent maksdato paa saker for person som ikke finnes`() {
        val sak = sakRepository.hentMaxdatoForSisteVedtak(ArenaPersonId(0xcafebabe.toInt()))
        assertThat(sak).isNull()
    }

}
