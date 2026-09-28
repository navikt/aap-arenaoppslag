package no.nav.aap.arenaoppslag.service

import io.mockk.every
import io.mockk.mockk
import no.nav.aap.arenaoppslag.database.MeldekortperiodeRepository
import no.nav.aap.arenaoppslag.database.VedtakRepository
import no.nav.aap.arenaoppslag.modeller.ArenaSak
import no.nav.aap.arenaoppslag.modeller.ArenaSakPerson
import no.nav.aap.arenaoppslag.modeller.ArenaVedtakRad
import no.nav.aap.arenaoppslag.modeller.KvotebrukHendelse
import no.nav.aap.arenaoppslag.modeller.Periode
import no.nav.aap.arenaoppslag.modeller.PersonId
import no.nav.aap.arenaoppslag.modeller.SakId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class MigreringServiceTest {

    private val vedtakRepository = mockk<VedtakRepository>()
    private val meldekortperiodeRepository = mockk<MeldekortperiodeRepository>()
    private val telleverkService = mockk<TelleverkService>()

    private val service = MigreringService(vedtakRepository, meldekortperiodeRepository, telleverkService)

    private val sakId = SakId(9001)
    private val idag = LocalDate.of(2024, 3, 10)
    private val sak = ArenaSak(
        sakId = "9001",
        opprettetAar = 2023,
        lopenr = 9001,
        person = ArenaSakPerson(
            personId = 100,
            fodselsnummer = "12345678901",
            fornavn = "Test",
            etternavn = "Testesen",
        ),
        statuskode = "AKTIV",
        statusnavn = "Aktiv",
        registrertDato = LocalDateTime.of(2023, 1, 1, 0, 0),
        avsluttetDato = null,
    )

    private fun vedtak(fraOgMed: LocalDate) = ArenaVedtakRad(
        vedtakId = 1,
        lopenrvedtak = 1,
        statusKode = "IVERK",
        statusNavn = "Iverksatt",
        vedtaktypeKode = "O",
        vedtaktypeNavn = "Ny rettighet",
        aktivitetsfaseKode = "IKKE",
        aktivitetsfaseNavn = "Ikke spesif. aktivitetsfase",
        fraOgMed = fraOgMed,
        tilDato = null,
        rettighetkode = "AAP",
        rettighetnavn = "Arbeidsavklaringspenger",
        utfallkode = "JA",
        begrunnelse = null,
        saksbehandler = null,
        beslutter = null,
        relatertVedtak = null,
    )

    private fun kvotebrukHendelse(id: Int, dato: LocalDate, resterende: Int, kode: String = "AAP") =
        KvotebrukHendelse(
            id = id,
            kvoteTypeKode = kode,
            endringsGrunnlag = "MKORT",
            objektIdGrunnlag = 5001L,
            antallBevegelse = -1,
            posteringTypeKode = "TREKK",
            datoHendelse = dato,
            resterende = resterende,
            modUser = null,
            begrunnelse = null,
        )

    @Test
    fun `henter soknadsdato, migreringsdato og gjenstaaende ordinaer kvote`() {
        every { vedtakRepository.hentForsteInnvilgetVedtakForSak(sakId) } returns vedtak(LocalDate.of(2023, 1, 1))
        every { meldekortperiodeRepository.hentGjeldendePeriode(idag) } returns
            Periode(LocalDate.of(2024, 3, 4), LocalDate.of(2024, 3, 17))
        every { telleverkService.hentKvoteBrukHendelserForPerson(PersonId(100)) } returns setOf(
            kvotebrukHendelse(id = 1, dato = LocalDate.of(2023, 2, 1), resterende = 250),
            kvotebrukHendelse(id = 2, dato = LocalDate.of(2024, 2, 1), resterende = 200),
            // Nyere hendelse enn migreringsdatoen skal ikke telle med i saldoen på migreringstidspunktet.
            kvotebrukHendelse(id = 3, dato = LocalDate.of(2024, 6, 1), resterende = 150),
            // Annen kvotetype skal ikke påvirke ordinær-saldoen.
            kvotebrukHendelse(id = 4, dato = LocalDate.of(2024, 3, 1), resterende = 999, kode = "MAAPU"),
        )

        val krav = service.hentKravForSak(sak, sakId, idag)

        assertThat(krav.lopenr).isEqualTo(9001)
        assertThat(krav.aar).isEqualTo(2023)
        assertThat(krav.soknadsdato).isEqualTo(LocalDate.of(2023, 1, 1))
        assertThat(krav.migreringsdato).isEqualTo(LocalDate.of(2024, 3, 4))
        assertThat(krav.gjenstaaendeOrdinaerKvote).isEqualTo(200)
    }

    @Test
    fun `soknadsdato er null når saken ikke har noe innvilget vedtak`() {
        every { vedtakRepository.hentForsteInnvilgetVedtakForSak(sakId) } returns null
        every { meldekortperiodeRepository.hentGjeldendePeriode(idag) } returns null

        val krav = service.hentKravForSak(sak, sakId, idag)

        assertThat(krav.soknadsdato).isNull()
        assertThat(krav.migreringsdato).isNull()
        assertThat(krav.gjenstaaendeOrdinaerKvote).isNull()
    }
}
