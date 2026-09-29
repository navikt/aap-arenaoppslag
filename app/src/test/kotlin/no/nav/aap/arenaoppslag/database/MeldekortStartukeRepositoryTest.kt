package no.nav.aap.arenaoppslag.database

import no.nav.aap.arenaoppslag.modeller.MeldekortStartuke
import no.nav.aap.arenaoppslag.modeller.PersonId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MeldekortStartukeRepositoryTest : H2TestBase("flyway/meldekort") {

    private val meldekortRepository by lazy { MeldekortRepository(h2) }

    @Test
    fun `henter år og periodekode fra siste ATTF-meldekort`() {
        val startuke = meldekortRepository.hentStartukeForSisteMeldekort(PersonId(500))

        assertThat(startuke).isEqualTo(MeldekortStartuke(aar = 2026, ukenummer = 53))
    }

    @Test
    fun `tolker periodekode med ledende null som ukenummer`() {
        val startuke = meldekortRepository.hentStartukeForSisteMeldekort(PersonId(503))

        assertThat(startuke).isEqualTo(MeldekortStartuke(aar = 2025, ukenummer = 5))
    }

    @Test
    fun `ignorerer meldekort i andre meldegrupper enn ATTF`() {
        val startuke = meldekortRepository.hentStartukeForSisteMeldekort(PersonId(501))

        assertThat(startuke).isNull()
    }

    @Test
    fun `returnerer null når personen ikke har meldekort`() {
        val startuke = meldekortRepository.hentStartukeForSisteMeldekort(PersonId(502))

        assertThat(startuke).isNull()
    }
}

