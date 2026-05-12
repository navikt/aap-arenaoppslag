package no.nav.aap.arenaoppslag.database

import no.nav.aap.arenaoppslag.modeller.ArenaSak
import no.nav.aap.arenaoppslag.modeller.ArenaSakOppsummering
import no.nav.aap.arenaoppslag.modeller.ArenaSakPerson
import no.nav.aap.arenaoppslag.modeller.Maksdatolinje
import no.nav.aap.arenaoppslag.modeller.PersonId
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
        // person_id=1 tilsvarer fnr "123" i minimumtest-datasettet
        val saker = sakRepository.hentSakerForPerson(PersonId(1))
        assertThat(saker).containsExactly(forventetSak)
    }

    @Test
    fun `hentSakerForPerson returnerer tom liste for ukjent person`() {
        val saker = sakRepository.hentSakerForPerson(PersonId(99999))
        assertThat(saker).isEmpty()
    }

    @Test
    fun `hentSakerForPerson returnerer tom liste for person uten saker`() {
        // Person "ingenvedtak" har person_id=3 i minimumtest-datasettet
        val saker = sakRepository.hentSakerForPerson(PersonId(3))
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
        // person_id=900 tilsvarer fnr "tosaker" i saklistetest-datasettet
        val saker = sakRepository.hentSakerForPerson(PersonId(900))
        assertThat(saker).containsExactlyInAnyOrderElementsOf(forventedeSaker)
    }

    @Test
    fun `hent maksdato paa saker for person finner forventede data`() {
        val saker = sakRepository.hentSakerMedMaksDatoOgVedtak(PersonId(100))
        assertThat(saker).hasSize(2)
        assertThat(saker).containsExactly(
            // mappingen testes
            Maksdatolinje(
                1_10_3, 1_10_3, "IKKE", "O",
                LocalDate.of(2022, 8, 30),
                LocalDate.of(2025, 12, 31),
                LocalDate.of(2025, 6, 30),
                sakRegistrert = LocalDate.of(2022, 2, 3),
                sakAvsluttet = null,
                sakStatus = "AKTIV"
            ),
            Maksdatolinje(
                1_10_2, 1_10_1, "IKKE", "O",
                LocalDate.of(2010, 8, 29),
                LocalDate.of(2026, 6, 30),
                LocalDate.of(2025, 6, 30),
                sakRegistrert = LocalDate.of(2022, 2, 2),
                sakAvsluttet = null,
                sakStatus = "AKTIV"
            ),
        )
    }

    @Test
    fun `hent maksdato paa saker for person som mangler data`() {
        val saker = sakRepository.hentSakerMedMaksDatoOgVedtak(PersonId(101))
        assertThat(saker).isEmpty()
    }

    @Test
    fun `hent maksdato paa saker for person som ikke finnes`() {
        val saker = sakRepository.hentSakerMedMaksDatoOgVedtak(PersonId(0xcafebabe.toInt()))
        assertThat(saker).isEmpty()
    }

}
