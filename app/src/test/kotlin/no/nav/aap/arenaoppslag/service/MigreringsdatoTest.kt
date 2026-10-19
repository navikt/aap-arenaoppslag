package no.nav.aap.arenaoppslag.service

import io.mockk.every
import io.mockk.mockk
import no.nav.aap.arenaoppslag.database.H2TestBase
import no.nav.aap.arenaoppslag.database.MeldekortperiodeRepository
import no.nav.aap.arenaoppslag.database.VedtakRepository
import no.nav.aap.arenaoppslag.modeller.ArenaSak
import no.nav.aap.arenaoppslag.modeller.ArenaSakPerson
import no.nav.aap.arenaoppslag.modeller.SakId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class MigreringsdatoTest : H2TestBase("flyway/meldeperiode") {

    private val vedtakRepository = mockk<VedtakRepository> {
        every { hentForsteInnvilgetVedtakForSak(any()) } returns null
    }
    private val telleverkService = mockk<TelleverkService> {
        every { hentKvoteBrukHendelserForPerson(any()) } returns emptySet()
    }

    private val service = MigreringService(
        vedtakRepository,
        MeldekortperiodeRepository(h2),
        telleverkService,
        mockk(),
        mockk(),
        mockk(),
    )

    // Onsdag i uke 34 dekkes både av perioden uke 33-34 og uke 34-35
    private val onsdagUke34 = LocalDate.of(2026, 8, 19)

    @Test
    fun `migreringsdato følger brukerens meldekortsyklus når to meldekortperioder overlapper dagens dato`() {
        val oddetallsbruker = service.hentKravForSak(sak(personId = 400), SakId(1), onsdagUke34)
        val partallsbruker = service.hentKravForSak(sak(personId = 401), SakId(2), onsdagUke34)

        assertThat(oddetallsbruker.migreringsdato).isEqualTo(LocalDate.of(2026, 8, 10))
        assertThat(partallsbruker.migreringsdato).isEqualTo(LocalDate.of(2026, 8, 17))
    }

    private fun sak(personId: Int) = ArenaSak(
        sakId = personId.toString(),
        opprettetAar = 2026,
        lopenr = personId,
        person = ArenaSakPerson(
            personId = personId,
            fodselsnummer = "${personId}00000000",
            fornavn = "Test",
            etternavn = "Testesen",
        ),
        statuskode = "AKTIV",
        statusnavn = "Aktiv",
        registrertDato = LocalDateTime.of(2026, 1, 1, 0, 0),
        avsluttetDato = null,
    )
}
