package no.nav.aap.arenaoppslag.database

import no.nav.aap.arenaoppslag.modeller.ArenaSak
import no.nav.aap.arenaoppslag.modeller.ArenaSakOppsummering
import no.nav.aap.arenaoppslag.modeller.ArenaSakPerson
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SakRepositoryTest : H2TestBase("flyway/minimumtest", "flyway/saklistetest") {
    @Test
    fun `klarer å hente en sak fra databasen`() {
        val forvantetSak = ArenaSak(
            sakId = "1",
            opprettetAar = 2021,
            lopenr = 1,
            statuskode = "INAKT",
            statusnavn = "Inaktiv",
            registrertDato = LocalDate.of(2022, 2, 2).atStartOfDay(),
            avsluttetDato = null,
            person = ArenaSakPerson(
                personId = 1,
                fodselsnummer = "123",
                fornavn = "Test",
                etternavn = "Bestesen",
            )
        )
        val sakRepository = SakRepository(h2)
        val sak = sakRepository.hentSak(1)
        assertThat(sak).isEqualTo(forvantetSak)
    }

    @Test
    fun `returnerer NULL om saken ikke finnes i databasen`() {
        val sakRepository = SakRepository(h2)
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
            regDato = LocalDate.of(2022, 2, 2),
            avsluttetDato = null,
        )
        val sakRepository = SakRepository(h2)
        val saker = sakRepository.hentSakerForPersnNummere(setOf("123"))
        assertThat(saker).containsExactly(forventetSak)
    }

    @Test
    fun `hentSakerForPersnNummere returnerer tom liste for ukjent person`() {
        val sakRepository = SakRepository(h2)
        val saker = sakRepository.hentSakerForPersnNummere(setOf("007"))
        assertThat(saker).isEmpty()
    }

    @Test
    fun `hentSakerForPersnNummere returnerer tom liste for person uten saker`() {
        // Person "ingenvedtak" (person_id=3) er registrert uten noen saker
        val sakRepository = SakRepository(h2)
        val saker = sakRepository.hentSakerForPersnNummere(setOf("ingenvedtak"))
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
                regDato = LocalDate.of(2020, 1, 15),
                avsluttetDato = null,
            ),
            ArenaSakOppsummering(
                sakId = "902",
                lopenummer = 2,
                aar = 2023,
                antallVedtak = 1,
                sakstype = "Arbeidsavklaringspenger",
                regDato = LocalDate.of(2023, 3, 1),
                avsluttetDato = LocalDate.of(2023, 12, 31),
            ),
        )
        val sakRepository = SakRepository(h2)
        val saker = sakRepository.hentSakerForPersnNummere(setOf("tosaker"))
        assertThat(saker).containsExactlyInAnyOrderElementsOf(forventedeSaker)
    }

    @Test
    fun `hentSakerForPersnNummere returnerer saker for flere personer i ett kall`() {
        val sakRepository = SakRepository(h2)
        val saker = sakRepository.hentSakerForPersnNummere(setOf("123", "tosaker"))
        // "123" har 1 sak, "tosaker" har 2 saker - totalt 3 saker
        assertThat(saker).hasSize(3)
        assertThat(saker.map { it.sakId }).containsExactlyInAnyOrder("1", "901", "902")
    }

    @Test
    fun `hentSakerForPersnNummere returnerer tom liste naar settet er tomt`() {
        val sakRepository = SakRepository(h2)
        val saker = sakRepository.hentSakerForPersnNummere(emptySet())
        assertThat(saker).isEmpty()
    }
}
