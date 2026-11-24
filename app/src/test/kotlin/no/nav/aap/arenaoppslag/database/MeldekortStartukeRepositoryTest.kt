package no.nav.aap.arenaoppslag.database

import no.nav.aap.arenaoppslag.modeller.MeldekortStartuke
import no.nav.aap.arenaoppslag.modeller.ArenaPersonId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MeldekortStartukeRepositoryTest : H2TestBase("flyway/meldekort") {

    private val meldekortRepository by lazy { MeldekortRepository(h2) }

    @Test
    fun `henter år og periodekode fra siste godkjente ATTF-meldekort`() {
        val startuke = meldekortRepository.hentStartukeForSisteMeldekort(ArenaPersonId(500))

        assertThat(startuke).isEqualTo(MeldekortStartuke(aar = 2026, ukenummer = 51))
    }

    @Test
    fun `henter startuke fra meldekort i meldegruppe FY`() {
        val startuke = meldekortRepository.hentStartukeForSisteMeldekort(ArenaPersonId(504))

        assertThat(startuke).isEqualTo(MeldekortStartuke(aar = 2026, ukenummer = 12))
    }

    @Test
    fun `tolker periodekode med ledende null som ukenummer`() {
        val startuke = meldekortRepository.hentStartukeForSisteMeldekort(ArenaPersonId(503))

        assertThat(startuke).isEqualTo(MeldekortStartuke(aar = 2025, ukenummer = 5))
    }

    @Test
    fun `ignorerer meldekort i andre meldegrupper enn FY og ATTF`() {
        val startuke = meldekortRepository.hentStartukeForSisteMeldekort(ArenaPersonId(501))

        assertThat(startuke).isNull()
    }

    @Test
    fun `ignorerer meldekort med annen beregningsstatus enn KLAR og FERDI`() {
        val startuke = meldekortRepository.hentStartukeForSisteMeldekort(ArenaPersonId(505))

        assertThat(startuke).isNull()
    }

    @Test
    fun `returnerer null når personen ikke har meldekort`() {
        val startuke = meldekortRepository.hentStartukeForSisteMeldekort(ArenaPersonId(502))

        assertThat(startuke).isNull()
    }
}
