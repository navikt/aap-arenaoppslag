package no.nav.aap.arenaoppslag.database

import no.nav.aap.arenaoppslag.modeller.PersonId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class KvotebrukRepositoryTest : H2TestBase("flyway/kvotebruktest") {

    private val repo = lazy { TelleverkRepository(h2) }

    @Test
    fun `hentKvoteForPerson returnerer tomt sett for ukjent person`() {
        val hendelser = repo.value.hentKvoteForPerson(PersonId(999))
        assertThat(hendelser).isEmpty()
    }

    @Test
    fun `hentKvoteForPerson returnerer riktig antall hendelser for kjent person`() {
        val hendelser = repo.value.hentKvoteForPerson(PersonId(4873545))
        assertThat(hendelser).hasSize(2)
    }

    @Test
    fun `hentKvoteForPerson returnerer korrekte feltverdier for AAP-hendelse`() {
        val hendelser = repo.value.hentKvoteForPerson(PersonId(4873545))
        val aap = hendelser.first { it.kvoteTypeKode == "AAP" }

        assertThat(aap.kvoteBrukId).isEqualTo(100)
        assertThat(aap.tabellnavnAliasGrunnlag).isEqualTo("VEDTAK")
        assertThat(aap.objektIdGrunnlag).isEqualTo(1)
        assertThat(aap.antallBevegelse).isEqualTo(20)
        assertThat(aap.posteringTypeKode).isEqualTo("ORD")
        assertThat(aap.begrunnelse).isEqualTo("Automatisk")
        assertThat(aap.verdi).isEqualTo(5280)
        assertThat(aap.kvoteTypeNavn).isEqualTo("Resterende AAP-periode med aktivitetsfase UA og AU")
        assertThat(aap.validerHeleDager).isTrue()
        assertThat(aap.datoFra).isNotNull()
        assertThat(aap.datoTil).isNotNull()
    }

    @Test
    fun `hentKvoteForPerson returnerer korrekte feltverdier for MAAPU-hendelse`() {
        val hendelser = repo.value.hentKvoteForPerson(PersonId(4873545))
        val maapu = hendelser.first { it.kvoteTypeKode == "MAAPU" }

        assertThat(maapu.kvoteBrukId).isEqualTo(101)
        assertThat(maapu.antallBevegelse).isEqualTo(10)
        assertThat(maapu.begrunnelse).isEqualTo("Utvidet")
        assertThat(maapu.verdi).isEqualTo(460)
        assertThat(maapu.kvoteTypeNavn).isEqualTo("Resterende unntaksperiode med aktivitetsfase UA og AU")
        assertThat(maapu.validerHeleDager).isTrue()
    }

    @Test
    fun `hentKvoteForPerson returnerer kun data for etterspurt person`() {
        val hendelserKjentPerson = repo.value.hentKvoteForPerson(PersonId(4873545))
        val hendelserAnnenPerson = repo.value.hentKvoteForPerson(PersonId(99999))

        assertThat(hendelserKjentPerson).hasSize(2)
        assertThat(hendelserAnnenPerson).hasSize(1)
        assertThat(hendelserKjentPerson.map { it.kvoteBrukId }).doesNotContainAnyElementsOf(
            hendelserAnnenPerson.map { it.kvoteBrukId }
        )
    }
}

