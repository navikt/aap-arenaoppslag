package no.nav.aap.arenaoppslag.database

import no.nav.aap.arenaoppslag.modeller.ArenaSaksopplysning
import no.nav.aap.arenaoppslag.modeller.ArenaSaksopplysningAttributt
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SaksopplysningRepositoryTest : H2TestBase("flyway/minimumtest") {

    private val repo = SaksopplysningRepository(h2)

    @Test
    fun `henter saksopplysninger for kjent vedtakId`() {
        val resultat = repo.hentForVedtakId(37067849)

        assertThat(resultat).hasSize(1)

        val saksopplysning = resultat.single()
        assertThat(saksopplysning.saksopplysningkode).isEqualTo("ARBEIDSEVNE")
        assertThat(saksopplysning.saksopplysningnavn).isEqualTo("Arbeidsevne")
        assertThat(saksopplysning.skjermbildetekst).isEqualTo("Vurdering av arbeidsevne")
        assertThat(saksopplysning.statusRepeterbar).isEqualTo("N")
        assertThat(saksopplysning.verdi).isEqualTo("NEI")
        assertThat(saksopplysning.attributter).hasSize(2)
    }

    @Test
    fun `attributter er sortert etter posisjon`() {
        val saksopplysning = repo.hentForVedtakId(37067849).single()

        val posisjoner = saksopplysning.attributter.map { it.posisjon }
        assertThat(posisjoner).isSorted
    }

    @Test
    fun `returnerer korrekte attributtverdier`() {
        val saksopplysning = repo.hentForVedtakId(37067849).single()

        val begrunnelse = saksopplysning.attributter.find { it.attributtkode == "BEGRUNNELSE" }!!
        assertThat(begrunnelse.verdi).isEqualTo("Bruker kan ikke jobbe")
        assertThat(begrunnelse.formatnavn).isEqualTo("TEKST")
        assertThat(begrunnelse.posisjon).isEqualTo(1)
        assertThat(begrunnelse.statusSjekketAv).isEqualTo("A")

        val dato = saksopplysning.attributter.find { it.attributtkode == "DATO" }!!
        assertThat(dato.verdi).isEqualTo("2022-08-30")
        assertThat(dato.formatnavn).isEqualTo("DATO")
        assertThat(dato.posisjon).isEqualTo(2)
        assertThat(dato.statusSjekketAv).isNull()
    }

    @Test
    fun `returnerer tom liste for ukjent vedtakId`() {
        val resultat = repo.hentForVedtakId(999999999)

        assertThat(resultat).isEmpty()
    }
}

