package no.nav.aap.arenaoppslag.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.arenaoppslag.database.SakRepository
import no.nav.aap.arenaoppslag.modeller.ArenaSakOppsummering
import no.nav.aap.arenaoppslag.modeller.Maksdatolinje
import no.nav.aap.arenaoppslag.modeller.PersonId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SakServiceTest {

    private val personId = PersonId(42)

    @Test
    fun `hentSakerForPerson mapper saker til kontrakt og cacher per person`() {
        val sakRepository = mockk<SakRepository>()
        every { sakRepository.hentSakerForPerson(personId) } returns listOf(
            arenaSakOppsummering(sakId = "1"),
            arenaSakOppsummering(sakId = "2"),
        )

        val service = SakService(sakRepository)

        val foersteKall = service.hentSakerForPerson(personId)
        val andreKall = service.hentSakerForPerson(personId)

        assertThat(foersteKall.saker.map { it.sakId }).containsExactly("1", "2")
        assertThat(andreKall).isSameAs(foersteKall)
        verify(exactly = 1) { sakRepository.hentSakerForPerson(personId) }
    }

    @Test
    fun `hentMaksdatoAapForVedtakISaker mapper maksdatolinjer til kontrakt`() {
        val sakRepository = mockk<SakRepository>()
        val maxdato = LocalDate.of(2026, 5, 1)
        every { sakRepository.hentSakerMedMaksDatoOgVedtak(personId) } returns listOf(
            maksdatolinje(sakId = 1, vedtaktypeKode = "O", sakStatus = "AKTIV", maxdato = maxdato),
        )

        val resultat = SakService(sakRepository).hentMaksdatoAapForVedtakISaker(personId)

        assertThat(resultat).hasSize(1)
        assertThat(resultat.single().sakId).isEqualTo(1)
        assertThat(resultat.single().lopendeVedtak).isTrue()
        assertThat(resultat.single().sisteVedtak.maxdatoAap).isEqualTo(maxdato)
    }

    @Test
    fun `hentMaksdatoAapForPerson returnerer seneste maksdato blant lopende vedtak`() {
        val sakRepository = mockk<SakRepository>()
        val tidlig = LocalDate.of(2026, 1, 1)
        val sen = LocalDate.of(2027, 6, 30)
        val senestMenIkkeLopende = LocalDate.of(2030, 1, 1)
        every { sakRepository.hentSakerMedMaksDatoOgVedtak(personId) } returns listOf(
            maksdatolinje(sakId = 1, vedtaktypeKode = "O", sakStatus = "AKTIV", maxdato = tidlig),
            maksdatolinje(sakId = 2, vedtaktypeKode = "E", sakStatus = "AKTIV", maxdato = sen),
            // Avsluttet sak med løpende vedtakstype regnes fortsatt som relevant maxdato,
            // men her er vedtaket stanset slik at det ikke teller som løpende
            maksdatolinje(sakId = 3, vedtaktypeKode = "S", sakStatus = "AVSLU", maxdato = senestMenIkkeLopende),
            // Ikke løpende (feil vedtakstype) - skal ignoreres
            maksdatolinje(sakId = 4, vedtaktypeKode = "S", sakStatus = "AKTIV", maxdato = senestMenIkkeLopende),
        )

        val resultat = SakService(sakRepository).hentMaksdatoAapForPerson(personId)

        assertThat(resultat).isEqualTo(sen)
    }

    @Test
    fun `hentMaksdatoAapForPerson returnerer null naar ingen lopende vedtak finnes`() {
        val sakRepository = mockk<SakRepository>()
        every { sakRepository.hentSakerMedMaksDatoOgVedtak(personId) } returns listOf(
            maksdatolinje(sakId = 1, vedtaktypeKode = "S", sakStatus = "AKTIV", maxdato = LocalDate.now()),
        )

        val resultat = SakService(sakRepository).hentMaksdatoAapForPerson(personId)

        assertThat(resultat).isNull()
    }

    @Test
    fun `hentMaksdatoAapForPerson returnerer null naar lopende vedtak mangler maksdato`() {
        val sakRepository = mockk<SakRepository>()
        every { sakRepository.hentSakerMedMaksDatoOgVedtak(personId) } returns listOf(
            maksdatolinje(sakId = 1, vedtaktypeKode = "O", sakStatus = "AKTIV", maxdato = null),
        )

        val resultat = SakService(sakRepository).hentMaksdatoAapForPerson(personId)

        assertThat(resultat).isNull()
    }

    private fun arenaSakOppsummering(sakId: String) = ArenaSakOppsummering(
        sakId = sakId,
        lopenummer = 1,
        aar = 2026,
        antallVedtak = 1,
        sakstype = "AAP",
        statuskode = "AKTIV",
        statusnavn = "Aktiv",
        regDato = LocalDate.of(2026, 1, 1),
        avsluttetDato = null,
    )

    private fun maksdatolinje(
        sakId: Int,
        vedtaktypeKode: String,
        sakStatus: String,
        maxdato: LocalDate?,
    ) = Maksdatolinje(
        sakId = sakId,
        vedtakId = sakId * 10,
        aktfaseKode = "FA",
        vedtaktypeKode = vedtaktypeKode,
        fra = LocalDate.of(2025, 1, 1),
        maxdatoUnntak = null,
        maxdato = maxdato,
        utvidetKvoteInnvilgetFra = null,
        sakRegistrert = LocalDate.of(2025, 1, 1),
        sakAvsluttet = null,
        sakStatus = sakStatus,
    )
}