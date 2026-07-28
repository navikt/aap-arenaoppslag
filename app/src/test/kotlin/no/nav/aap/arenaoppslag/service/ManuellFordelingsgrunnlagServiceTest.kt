package no.nav.aap.arenaoppslag.service

import io.mockk.every
import io.mockk.mockk
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SakMedSisteVedtakOgMaksdato
import no.nav.aap.arenaoppslag.kontrakt.apiv1.VedtakMedMaksdato
import no.nav.aap.arenaoppslag.modeller.PersonId
import no.nav.aap.arenaoppslag.modeller.TelleverkForPerson
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ManuellFordelingsgrunnlagServiceTest {

    private val sakService: SakService = mockk()
    private val posteringService: PosteringService = mockk()
    private val telleverkService: TelleverkService = mockk()
    private val service = ManuellFordelingsgrunnlagService(sakService, posteringService, telleverkService)

    private val personId = PersonId(1)
    private val iDag = LocalDate.of(2026, 1, 1)

    private fun sak(
        maxdatoOrdinaer: LocalDate? = null,
        maxdatoUnntak: LocalDate? = null,
        vedtaktypeKode: String = "O",
        sakStatus: String = "AKTIV",
        til: LocalDate? = null,
    ) = SakMedSisteVedtakOgMaksdato(
        sakId = 42,
        saknummer = "2024-23456",
        sakStatus = sakStatus,
        sakRegistrert = LocalDate.of(2024, 1, 1),
        sakAvsluttet = null,
        unntaksvilkaarInnvilget = null,
        unntaksvilkaarGjelderFra = null,
        sisteVedtak = VedtakMedMaksdato(
            vedtakId = 1,
            aktfaseKode = "UVUP",
            vedtaktypeKode = vedtaktypeKode,
            fra = LocalDate.of(2024, 1, 1),
            til = til,
            maxdatoOrdinaer = maxdatoOrdinaer,
            maxdatoUnntak = maxdatoUnntak,
            maxdatoAap = maxdatoOrdinaer,
        ),
    )

    @Test
    fun `beregner gjenstaaende dager og aktiv sak`() {
        every { sakService.hentMaksdatoAapMedVedtakOgSak(personId) } returns
            sak(maxdatoOrdinaer = iDag.plusDays(67), maxdatoUnntak = iDag.plusDays(10), til = iDag.minusWeeks(10))
        every { posteringService.hentSisteAapUtbetalingForPerson(personId) } returns iDag.minusWeeks(10)
        every { telleverkService.hentTelleverkForPerson(personId) } returns
            TelleverkForPerson(ordineerAAPKvote = 67, utvidetAAPKvote = 10)

        val grunnlag = requireNotNull(service.hentManuellFordelingsgrunnlag(personId, iDag))

        assertThat(grunnlag.saksnummer).isEqualTo("2024-23456")
        assertThat(grunnlag.erAktiv).isTrue()
        assertThat(grunnlag.gjenståendeOrdinæreDager).isEqualTo(67)
        assertThat(grunnlag.gjenståendeUnntaksDager).isEqualTo(10)
        assertThat(grunnlag.under52Uker).isTrue()
        assertThat(grunnlag.sisteUtbetaling).isEqualTo(iDag.minusWeeks(10))
    }

    @Test
    fun `løpende vedtak uten til-dato regnes som under 52 uker`() {
        every { sakService.hentMaksdatoAapMedVedtakOgSak(personId) } returns sak(til = null)
        every { posteringService.hentSisteAapUtbetalingForPerson(personId) } returns null
        every { telleverkService.hentTelleverkForPerson(personId) } returns null

        val grunnlag = requireNotNull(service.hentManuellFordelingsgrunnlag(personId, iDag))

        assertThat(grunnlag.under52Uker).isTrue()
    }

    @Test
    fun `mangler telleverk gir null gjenstaaende dager`() {
        every { sakService.hentMaksdatoAapMedVedtakOgSak(personId) } returns
            sak(maxdatoOrdinaer = iDag.minusDays(5))
        every { posteringService.hentSisteAapUtbetalingForPerson(personId) } returns null
        every { telleverkService.hentTelleverkForPerson(personId) } returns null

        val grunnlag = requireNotNull(service.hentManuellFordelingsgrunnlag(personId, iDag))

        assertThat(grunnlag.gjenståendeOrdinæreDager).isNull()
        assertThat(grunnlag.gjenståendeUnntaksDager).isNull()
    }

    @Test
    fun `siste vedtak til-dato eldre enn 52 uker gir under52Uker false`() {
        every { sakService.hentMaksdatoAapMedVedtakOgSak(personId) } returns sak(til = iDag.minusWeeks(53))
        every { posteringService.hentSisteAapUtbetalingForPerson(personId) } returns null
        every { telleverkService.hentTelleverkForPerson(personId) } returns null

        val grunnlag = requireNotNull(service.hentManuellFordelingsgrunnlag(personId, iDag))

        assertThat(grunnlag.under52Uker).isFalse()
    }

    @Test
    fun `stansvedtak gir ikke aktiv sak`() {
        every { sakService.hentMaksdatoAapMedVedtakOgSak(personId) } returns
            sak(vedtaktypeKode = "S")
        every { posteringService.hentSisteAapUtbetalingForPerson(personId) } returns null
        every { telleverkService.hentTelleverkForPerson(personId) } returns null

        val grunnlag = requireNotNull(service.hentManuellFordelingsgrunnlag(personId, iDag))

        assertThat(grunnlag.erAktiv).isFalse()
    }

    @Test
    fun `person uten AAP-sak gir null grunnlag`() {
        every { sakService.hentMaksdatoAapMedVedtakOgSak(personId) } returns null

        val grunnlag = service.hentManuellFordelingsgrunnlag(personId, iDag)

        assertThat(grunnlag).isNull()
    }
}

