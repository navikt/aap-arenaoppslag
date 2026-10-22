package no.nav.aap.arenaoppslag.service

import io.mockk.every
import io.mockk.mockk
import no.nav.aap.arenaoppslag.database.MeldekortRepository
import no.nav.aap.arenaoppslag.modeller.MeldekortStartuke
import no.nav.aap.arenaoppslag.modeller.PersonId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MeldekortServiceTest {

    private val meldekortRepository = mockk<MeldekortRepository>()
    private val service = MeldekortService(meldekortRepository)

    @Test
    fun `hentStartukeForSisteMeldekort returnerer startuke fra repository`() {
        val personId = PersonId(500)
        every { meldekortRepository.hentStartukeForSisteMeldekort(personId) } returns
                MeldekortStartuke(aar = 2026, ukenummer = 53)

        assertThat(service.hentStartukeForSisteMeldekort(personId))
            .isEqualTo(MeldekortStartuke(aar = 2026, ukenummer = 53))
    }

    @Test
    fun `hentStartukeForSisteMeldekort returnerer null når personen ikke har meldekort`() {
        val personId = PersonId(502)
        every { meldekortRepository.hentStartukeForSisteMeldekort(personId) } returns null

        assertThat(service.hentStartukeForSisteMeldekort(personId)).isNull()
    }
}

