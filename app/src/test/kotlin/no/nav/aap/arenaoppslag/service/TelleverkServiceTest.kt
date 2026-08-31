package no.nav.aap.arenaoppslag.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.arenaoppslag.database.TelleverkRepository
import no.nav.aap.arenaoppslag.modeller.KvoteVerdi
import no.nav.aap.arenaoppslag.modeller.KvotebrukHendelse
import no.nav.aap.arenaoppslag.modeller.PersonId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TelleverkServiceTest {

    private val telleverkRepository = mockk<TelleverkRepository>()
    private val service = TelleverkService(telleverkRepository)

    @Test
    fun `hentTelleverkForPerson bruker cache ved andre kall`() {
        val personId = PersonId(100)
        every { telleverkRepository.hentTelleverkForPerson(personId) } returns setOf(
            KvoteVerdi(kode = "AAP", verdi = 4),
            KvoteVerdi(kode = "MAAPU", verdi = 25),
        )

        val forste = service.hentTelleverkForPerson(personId)
        val andre = service.hentTelleverkForPerson(personId)

        assertThat(forste?.ordineerAAPKvote).isEqualTo(4)
        assertThat(forste?.utvidetAAPKvote).isEqualTo(25)
        assertThat(andre).isEqualTo(forste)
        verify(exactly = 1) { telleverkRepository.hentTelleverkForPerson(personId) }
    }

    @Test
    fun `hentTelleverkForPerson cacher ikke null-resultat`() {
        val personId = PersonId(101)
        every { telleverkRepository.hentTelleverkForPerson(personId) } returns emptySet()

        assertThat(service.hentTelleverkForPerson(personId)).isNull()
        assertThat(service.hentTelleverkForPerson(personId)).isNull()

        verify(exactly = 2) { telleverkRepository.hentTelleverkForPerson(personId) }
    }

    @Test
    fun `hentTelleverkForPerson cacher uavhengig per person`() {
        val personEn = PersonId(102)
        val personTo = PersonId(103)
        every { telleverkRepository.hentTelleverkForPerson(personEn) } returns setOf(KvoteVerdi("AAP", 4))
        every { telleverkRepository.hentTelleverkForPerson(personTo) } returns setOf(KvoteVerdi("AAP", 9))

        assertThat(service.hentTelleverkForPerson(personEn)?.ordineerAAPKvote).isEqualTo(4)
        assertThat(service.hentTelleverkForPerson(personTo)?.ordineerAAPKvote).isEqualTo(9)

        verify(exactly = 1) { telleverkRepository.hentTelleverkForPerson(personEn) }
        verify(exactly = 1) { telleverkRepository.hentTelleverkForPerson(personTo) }
    }

    @Test
    fun `hentKvoteBrukHendelserForPerson bruker cache ved andre kall`() {
        val personId = PersonId(104)
        val hendelser = setOf(
            KvotebrukHendelse(
                id = 1,
                kvoteTypeKode = "AAP",
                endringsGrunnlag = "MKORT",
                objektIdGrunnlag = 5001,
                antallBevegelse = 0,
                posteringTypeKode = "OPPD",
                datoHendelse = LocalDate.of(2023, 1, 1),
                resterende = 10,
                modUser = null,
                begrunnelse = null,
            ),
        )
        every { telleverkRepository.hentKvoteBrukHendelserForPerson(personId) } returns hendelser

        assertThat(service.hentKvoteBrukHendelserForPerson(personId)).isEqualTo(hendelser)
        assertThat(service.hentKvoteBrukHendelserForPerson(personId)).isEqualTo(hendelser)

        verify(exactly = 1) { telleverkRepository.hentKvoteBrukHendelserForPerson(personId) }
    }

    @Test
    fun `hentKvoteBrukHendelserForPerson cacher ogsaa tomt resultat`() {
        val personId = PersonId(105)
        every { telleverkRepository.hentKvoteBrukHendelserForPerson(personId) } returns emptySet()

        assertThat(service.hentKvoteBrukHendelserForPerson(personId)).isEmpty()
        assertThat(service.hentKvoteBrukHendelserForPerson(personId)).isEmpty()

        verify(exactly = 1) { telleverkRepository.hentKvoteBrukHendelserForPerson(personId) }
    }
}

