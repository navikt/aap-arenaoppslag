package no.nav.aap.arenaoppslag.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.arenaoppslag.database.MedisinskOpplysningRepository
import no.nav.aap.arenaoppslag.database.MeldekortperiodeRepository
import no.nav.aap.arenaoppslag.database.VedtakRepository
import no.nav.aap.arenaoppslag.database.VilkårsvurderingRepository
import no.nav.aap.arenaoppslag.kontrakt.migrering.ArenaDiagnose
import no.nav.aap.arenaoppslag.modeller.ArenaSak
import no.nav.aap.arenaoppslag.modeller.ArenaSakPerson
import no.nav.aap.arenaoppslag.modeller.ArenaVedtakRad
import no.nav.aap.arenaoppslag.modeller.ArenaVilkårsvurdering
import no.nav.aap.arenaoppslag.modeller.KvotebrukHendelse
import no.nav.aap.arenaoppslag.modeller.MedisinskOpplysning
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
    private val vilkårsvurderingRepository = mockk<VilkårsvurderingRepository>()
    private val medisinskOpplysningRepository = mockk<MedisinskOpplysningRepository>()

    private val service = MigreringService(
        vedtakRepository,
        meldekortperiodeRepository,
        telleverkService,
        vilkårsvurderingRepository,
        medisinskOpplysningRepository,
    )

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

    private fun kvotebrukHendelse(
        id: Int,
        dato: LocalDate,
        resterende: Int,
        kode: String = "AAP",
        meldekortId: Long = 5001L,
        antallBevegelse: Int = -1,
        posteringType: String = "OPPD",
        grunnlag: String = "MKORT",
    ) = KvotebrukHendelse(
        id = id,
        kvoteTypeKode = kode,
        endringsGrunnlag = grunnlag,
        objektIdGrunnlag = meldekortId,
        antallBevegelse = antallBevegelse,
        posteringTypeKode = posteringType,
        datoHendelse = dato,
        resterende = resterende,
        modUser = null,
        begrunnelse = null,
    )

    private fun initHendelse(id: Int, dato: LocalDate, dager: Int) = kvotebrukHendelse(
        id = id, dato = dato, resterende = dager * 20, meldekortId = 1L,
        antallBevegelse = dager * 20, posteringType = "INIT", grunnlag = "VEDTAK",
    )

    @Test
    fun `henter soknadsdato, migreringsdato og gjenstaaende ordinaer kvote`() {
        every { vedtakRepository.hentForsteInnvilgetVedtakForSak(sakId) } returns vedtak(LocalDate.of(2023, 1, 1))
        every { meldekortperiodeRepository.hentGjeldendePeriode(idag) } returns
            Periode(LocalDate.of(2024, 3, 4), LocalDate.of(2024, 3, 17))
        every { meldekortperiodeRepository.hentPerioderForMeldekort(any()) } returns
            mapOf(5001L to Periode(LocalDate.of(2024, 2, 19), LocalDate.of(2024, 3, 3)))
        every { telleverkService.hentKvoteBrukHendelserForPerson(PersonId(100)) } returns setOf(
            kvotebrukHendelse(id = 1, dato = LocalDate.of(2023, 2, 1), resterende = 15680),
            kvotebrukHendelse(id = 2, dato = LocalDate.of(2024, 2, 1), resterende = 15480),
            // Meldekort behandlet etter migreringsdatoen, men for en periode før, skal trekkes fra kvoten.
            kvotebrukHendelse(id = 3, dato = LocalDate.of(2024, 3, 12), resterende = 14340),
            // Annen kvotetype skal ikke påvirke ordinær-saldoen.
            kvotebrukHendelse(id = 4, dato = LocalDate.of(2024, 3, 1), resterende = 999, kode = "MAAPU"),
        )

        val krav = service.hentKravForSak(sak, sakId, idag)

        assertThat(krav.lopenr).isEqualTo(9001)
        assertThat(krav.aar).isEqualTo(2023)
        assertThat(krav.soknadsdato).isEqualTo(LocalDate.of(2023, 1, 1))
        assertThat(krav.migreringsdato).isEqualTo(LocalDate.of(2024, 3, 4))
        assertThat(krav.gjenstaaendeOrdinaerKvote).isEqualTo(717)
    }

    @Test
    fun `meldekort behandlet etter migreringsdatoen trekkes fra gjenstaaende ordinaer kvote`() {
        // Gjenskaper feil der kvoten ble 784 i stedet for 717 fordi alle meldekort-trekk
        // hadde DATO_HENDELSE etter migreringsdatoen og ble filtrert bort.
        val idag = LocalDate.of(2026, 10, 1)
        val migreringsdato = LocalDate.of(2026, 9, 21)
        every { vedtakRepository.hentForsteInnvilgetVedtakForSak(sakId) } returns vedtak(LocalDate.of(2026, 4, 23))
        every { meldekortperiodeRepository.hentGjeldendePeriode(idag) } returns
            Periode(migreringsdato, LocalDate.of(2026, 10, 4))
        // Alle meldekortene gjelder perioder før migreringsdatoen.
        every { meldekortperiodeRepository.hentPerioderForMeldekort(any()) } answers {
            firstArg<Collection<Long>>().associateWith { id ->
                val fra = migreringsdato.minusWeeks(2 * (9 - id))
                Periode(fra, fra.plusDays(13))
            }
        }
        val behandlet = LocalDate.of(2026, 10, 1)
        val saldoer = listOf(784, 777, 767, 757, 747, 737, 727, 717)
        every { telleverkService.hentKvoteBrukHendelserForPerson(PersonId(100)) } returns setOf(
            initHendelse(id = 1, dato = LocalDate.of(2026, 6, 18), dager = saldoer.first()),
        ) + saldoer.zipWithNext().mapIndexed { i, (forrige, saldo) ->
            kvotebrukHendelse(
                id = i + 2,
                dato = behandlet,
                resterende = saldo * 20,
                meldekortId = (i + 2).toLong(),
                antallBevegelse = (saldo - forrige) * 20,
            )
        }

        val krav = service.hentKravForSak(sak, sakId, idag)

        assertThat(krav.soknadsdato).isEqualTo(LocalDate.of(2026, 4, 23))
        assertThat(krav.migreringsdato).isEqualTo(migreringsdato)
        assertThat(krav.gjenstaaendeOrdinaerKvote).isEqualTo(717)
    }

    @Test
    fun `trekk for meldeperioder fra og med migreringsdatoen telles ikke med i kvoten`() {
        // Tre sammenhengende meldeperioder à to uker (uke 1-2, 3-4 og 5-6). Alle meldekortene behandles
        // en måned etter migreringstidspunktet, onsdag i uke 4. Migreringsdatoen blir da starten på
        // meldeperiode 2, så kun trekket for meldeperiode 1 skal telle med.
        val meldeperiode1 = Periode(LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 16))
        val meldeperiode2 = Periode(LocalDate.of(2026, 8, 17), LocalDate.of(2026, 8, 30))
        val meldeperiode3 = Periode(LocalDate.of(2026, 8, 31), LocalDate.of(2026, 9, 13))
        val onsdagUke4 = LocalDate.of(2026, 8, 26)
        val behandlet = onsdagUke4.plusMonths(1)

        every { vedtakRepository.hentForsteInnvilgetVedtakForSak(sakId) } returns vedtak(LocalDate.of(2026, 4, 23))
        every { meldekortperiodeRepository.hentGjeldendePeriode(onsdagUke4) } returns meldeperiode2
        every { meldekortperiodeRepository.hentPerioderForMeldekort(any()) } returns mapOf(
            101L to meldeperiode1,
            102L to meldeperiode2,
            103L to meldeperiode3,
        )
        every { telleverkService.hentKvoteBrukHendelserForPerson(PersonId(100)) } returns setOf(
            initHendelse(id = 1, dato = LocalDate.of(2026, 6, 18), dager = 110),
            kvotebrukHendelse(
                id = 2, dato = behandlet, resterende = 100 * 20, meldekortId = 101L, antallBevegelse = -10 * 20,
            ),
            kvotebrukHendelse(
                id = 3, dato = behandlet, resterende = 90 * 20, meldekortId = 102L, antallBevegelse = -10 * 20,
            ),
            kvotebrukHendelse(
                id = 4, dato = behandlet, resterende = 80 * 20, meldekortId = 103L, antallBevegelse = -10 * 20,
            ),
        )

        val krav = service.hentKravForSak(sak, sakId, onsdagUke4)

        assertThat(krav.migreringsdato).isEqualTo(meldeperiode2.fraOgMedDato)
        assertThat(krav.gjenstaaendeOrdinaerKvote).isEqualTo(100)
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

    private fun vilkårsvurdering(id: Long, kode: String, statuskode: String, begrunnelse: String? = null) =
        ArenaVilkårsvurdering(
            vilkårsvurderingId = id,
            vilkårkode = kode,
            begrunnelse = begrunnelse,
            vurdertAv = "TEST01",
            vilkårnavn = kode,
            erObligatorisk = true,
            hjelpetekstUrl = null,
            lovtekstUrl = null,
            rundskrivUrl = null,
            statuskode = statuskode,
            statusnavn = statuskode,
        )

    @Test
    fun `henter begrunnelse og vilkår fra gjeldende 11-5-vedtak`() {
        val vedtak115 = vedtak(LocalDate.of(2024, 1, 1))
            .copy(vedtakId = 115, rettighetkode = "AA115", begrunnelse = "Nedsatt arbeidsevne")
        every { vedtakRepository.hentGjeldende115VedtakForSak(sakId, idag) } returns vedtak115
        every { vilkårsvurderingRepository.hentForVedtakIder(listOf(115)) } returns mapOf(
            115 to listOf(
                vilkårsvurdering(1, "SYKSKADLYT", "J", begrunnelse = "Legeerklæring foreligger"),
                vilkårsvurdering(2, "INNTNEDS", "N"),
                vilkårsvurdering(3, "AAARBEVNE", "V"),
            )
        )
        every { medisinskOpplysningRepository.hentForPerson(PersonId(100)) } returns emptyList()

        val respons = service.hentSykdomsvurderingForSak(sak, sakId, idag)!!.tilKontrakt()

        assertThat(respons.vedtakId).isEqualTo(115)
        assertThat(respons.begrunnelse).isEqualTo("Nedsatt arbeidsevne")
        assertThat(respons.vilkar.map { listOf(it.id, it.kode, it.status, it.begrunnelse) })
            .containsExactly(
                listOf(1L, "SYKSKADLYT", "J", "Legeerklæring foreligger"),
                listOf(2L, "INNTNEDS", "N", null),
                listOf(3L, "AAARBEVNE", "V", null),
            )
    }

    @Test
    fun `gir tom vilkårsliste når 11-5-vedtaket mangler vilkårsvurderinger`() {
        every { vedtakRepository.hentGjeldende115VedtakForSak(sakId, idag) } returns vedtak(idag).copy(vedtakId = 115)
        every { vilkårsvurderingRepository.hentForVedtakIder(listOf(115)) } returns emptyMap()
        every { medisinskOpplysningRepository.hentForPerson(PersonId(100)) } returns emptyList()

        val sykdomsvurdering = service.hentSykdomsvurderingForSak(sak, sakId, idag)

        assertThat(sykdomsvurdering?.vilkar).isEmpty()
        assertThat(sykdomsvurdering?.diagnoser).isEmpty()
    }

    @Test
    fun `sender diagnosene videre med Arena-kodene uendret`() {
        every { vedtakRepository.hentGjeldende115VedtakForSak(sakId, idag) } returns vedtak(idag).copy(vedtakId = 115)
        every { vilkårsvurderingRepository.hentForVedtakIder(listOf(115)) } returns emptyMap()
        every { medisinskOpplysningRepository.hentForPerson(PersonId(100)) } returns listOf(
            MedisinskOpplysning(1, "ICPC2", "L84", "HOVED", LocalDate.of(2023, 2, 1)),
            MedisinskOpplysning(2, "ICD10", "M54", "BI", LocalDate.of(2023, 3, 1)),
        )

        val respons = service.hentSykdomsvurderingForSak(sak, sakId, idag)!!.tilKontrakt()

        assertThat(respons.diagnoser).containsExactly(
            ArenaDiagnose("ICPC2", "L84", "HOVED", LocalDate.of(2023, 2, 1)),
            ArenaDiagnose("ICD10", "M54", "BI", LocalDate.of(2023, 3, 1)),
        )
    }

    @Test
    fun `returnerer null når saken mangler gjeldende 11-5-vedtak`() {
        every { vedtakRepository.hentGjeldende115VedtakForSak(sakId, idag) } returns null

        assertThat(service.hentSykdomsvurderingForSak(sak, sakId, idag)).isNull()
        verify(exactly = 0) { medisinskOpplysningRepository.hentForPerson(any()) }
    }
}
