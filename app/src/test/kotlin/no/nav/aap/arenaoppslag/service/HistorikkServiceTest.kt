package no.nav.aap.arenaoppslag.service

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.arenaoppslag.SakOgVedtakService
import no.nav.aap.arenaoppslag.kontrakt.apiv1.ArenaVedtakfakta
import no.nav.aap.arenaoppslag.kontrakt.apiv1.HistorikkStatusDetaljResponse
import no.nav.aap.arenaoppslag.modeller.ArenaSakMedVedtak
import no.nav.aap.arenaoppslag.modeller.ArenaSakPerson
import no.nav.aap.arenaoppslag.modeller.ArenaVedtakMedDetaljer
import no.nav.aap.arenaoppslag.modeller.ArenaVedtakMedMaxdato
import no.nav.aap.arenaoppslag.modeller.PersonId
import no.nav.aap.arenaoppslag.modeller.Saksnummer
import no.nav.aap.arenaoppslag.modeller.TelleverkForPerson
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.time.LocalDate

class HistorikkServiceTest {
    private val vedtakService = mockk<VedtakService>()
    private val utbetalingService = mockk<PosteringService>()
    private val telleverkService = mockk<TelleverkService>()
    private val sakOgVedtakService = mockk<SakOgVedtakService>()
    private val vedtakfaktaService = mockk<VedtakfaktaService>()
    private val service = HistorikkService(
        mockk(),
        vedtakService,
        utbetalingService,
        telleverkService,
        sakOgVedtakService,
        vedtakfaktaService,
    )
    private val personId = PersonId(1)
    private val saksnummer = Saksnummer(lopenummerSak = 17, aar = 2026)
    private val sluttdato = LocalDate.of(2026, 1, 31)
    private val unntaksdato = sluttdato.plusDays(1)
    private val logger = LoggerFactory.getLogger(HistorikkService::class.java) as Logger
    private val opprinneligLoggnivå = logger.level
    private val logg = ListAppender<ILoggingEvent>()

    @BeforeEach
    fun setup() {
        every { utbetalingService.hentSisteAapUtbetalingForPerson(personId) } returns null
        every { telleverkService.hentTelleverkForPerson(personId) } returns null
        every { vedtakfaktaService.hentForVedtakliste(emptyList()) } returns emptyMap()
        klargjørSak()
        logg.start()
        logger.level = Level.WARN
        logger.addAppender(logg)
    }

    @AfterEach
    fun cleanup() {
        logger.detachAppender(logg)
        logger.level = opprinneligLoggnivå
        logg.stop()
    }

    @Test
    fun `historikkstatus henter saken med sakens løpenummer og ikke vedtakets`() {
        val detaljer = requireNotNull(service.hentHistorikkStatusDetalj(personId).sisteSakOgVedtak)

        assertThat(detaljer.sakId).isEqualTo(42)
        assertThat(detaljer.saknummer).isEqualTo("2026-17")
        assertThat(detaljer.sisteVedtak.vedtakId).isEqualTo(1)
        verify(exactly = 1) { sakOgVedtakService.hentSakMedVedtak(saksnummer) }
    }

    @Test
    fun `historikkstatus bevarer bare vedtak med gyldige eller manglende datoer`() {
        val vedtak = egetVedtak().copy(aktivitetsfaseKode = "AKT")
        val vedtakMedUlikeDatoer = listOf(
            vedtak,
            vedtak.copy(vedtakId = 3, tilDato = unntaksdato),
            vedtak.copy(vedtakId = 4, fraOgMed = null),
            vedtak.copy(vedtakId = 5, tilDato = null),
            vedtak.copy(vedtakId = 6, fraOgMed = null, tilDato = null),
            vedtak.copy(vedtakId = 7, tilDato = unntaksdato.minusDays(1)),
        )
        klargjørSak(vedtakMedUlikeDatoer)
        val opprinneligSak = requireNotNull(sakOgVedtakService.hentSakMedVedtak(saksnummer))

        val detaljer = requireNotNull(service.hentHistorikkStatusDetalj(personId).sisteSakOgVedtak)

        assertThat(detaljer.vedtakene.map { it.vedtakId }).containsExactly(2, 3, 4, 5, 6)
        assertThat(opprinneligSak.vedtak).containsExactlyElementsOf(vedtakMedUlikeDatoer)
    }

    @Test
    fun `bruker fakta fra eget 11-12-vedtak for begge aktivitetsfaser`() {
        for (fase in listOf("AU", "UA")) {
            val vedtak = egetVedtak().copy(aktivitetsfaseKode = fase)
            klargjørSak(listOf(vedtak))
            every { vedtakfaktaService.hentForVedtakliste(listOf(vedtak.vedtakId)) } returns
                mapOf(vedtak.vedtakId to fakta("J"))

            val detaljer = requireNotNull(service.hentHistorikkStatusDetalj(personId).sisteSakOgVedtak)

            assertThat(detaljer.unntaksvilkaarInnvilget).describedAs(fase).isTrue()
            assertThat(detaljer.unntaksvilkaarGjelderFra).describedAs(fase).isEqualTo(unntaksdato)
        }
    }

    @Test
    fun `avslag i eget 11-12-vedtak gir false`() {
        val vedtak = egetVedtak()
        klargjørSak(listOf(vedtak))
        every { vedtakfaktaService.hentForVedtakliste(listOf(vedtak.vedtakId)) } returns
            mapOf(vedtak.vedtakId to fakta("N"))

        val detaljer = requireNotNull(service.hentHistorikkStatusDetalj(personId).sisteSakOgVedtak)

        assertThat(detaljer.unntaksvilkaarInnvilget).isFalse()
        assertThat(detaljer.unntaksvilkaarGjelderFra).isEqualTo(unntaksdato)
    }

    @Test
    fun `bruker fakta fra stansvedtak for begge aktivitetsfaser og unntaksutfall`() {
        for (fase in listOf("AU", "UA")) {
            val vedtak = stansvedtak().copy(aktivitetsfaseKode = fase)
            klargjørSak(listOf(vedtak))

            for ((verdi, innvilget) in listOf("J" to true, "N" to false)) {
                every { vedtakfaktaService.hentForVedtakliste(listOf(vedtak.vedtakId)) } returns
                    mapOf(vedtak.vedtakId to fakta(verdi))

                val detaljer = requireNotNull(service.hentHistorikkStatusDetalj(personId).sisteSakOgVedtak)

                assertThat(detaljer.unntaksvilkaarInnvilget).describedAs("$fase/$verdi").isEqualTo(innvilget)
                assertThat(detaljer.unntaksvilkaarGjelderFra).describedAs("$fase/$verdi").isEqualTo(unntaksdato)
            }
        }
    }

    @Test
    fun `gammel prosedyre har forrang foran fakta fra eget vedtak og stansvedtak`() {
        val vedtak = egetVedtak()
        val stans = stansvedtak()
        every { vedtakfaktaService.hentForVedtakliste(listOf(vedtak.vedtakId)) } returns
            mapOf(vedtak.vedtakId to fakta("J"))
        every { vedtakfaktaService.hentForVedtakliste(listOf(stans.vedtakId)) } returns
            mapOf(stans.vedtakId to fakta("N"))

        for (innvilget in listOf(true, false)) {
            klargjørSak(
                listOf(vedtak, stans),
                løpendeVedtak().copy(
                    unntaksvilkaarInnvilget = innvilget,
                    unntaksvilkaarGjelderFra = sluttdato,
                )
            )

            val detaljer = requireNotNull(service.hentHistorikkStatusDetalj(personId).sisteSakOgVedtak)

            assertThat(detaljer.unntaksvilkaarInnvilget).isEqualTo(innvilget)
            assertThat(detaljer.unntaksvilkaarGjelderFra).isEqualTo(sluttdato)
        }
    }

    @Test
    fun `gammel prosedyre beholder status selv om unntaksdato mangler`() {
        val vedtak = egetVedtak()
        klargjørSak(
            listOf(vedtak),
            løpendeVedtak().copy(unntaksvilkaarInnvilget = true),
        )
        every { vedtakfaktaService.hentForVedtakliste(listOf(vedtak.vedtakId)) } returns
            mapOf(vedtak.vedtakId to fakta("N"))

        val detaljer = requireNotNull(service.hentHistorikkStatusDetalj(personId).sisteSakOgVedtak)

        assertThat(detaljer.unntaksvilkaarInnvilget).isTrue()
        assertThat(detaljer.unntaksvilkaarGjelderFra).isNull()
        assertThat(logg.list).isEmpty()
    }

    @Test
    fun `eget vedtak har forrang foran nyere stansvedtak`() {
        val vedtak = egetVedtak()
        val stans = stansvedtak().copy(fraOgMed = unntaksdato.plusDays(10))
        klargjørSak(listOf(stans, vedtak))
        every { vedtakfaktaService.hentForVedtakliste(listOf(vedtak.vedtakId)) } returns
            mapOf(vedtak.vedtakId to fakta("N"))
        every { vedtakfaktaService.hentForVedtakliste(listOf(stans.vedtakId)) } returns
            mapOf(stans.vedtakId to fakta("J"))

        val detaljer = requireNotNull(service.hentHistorikkStatusDetalj(personId).sisteSakOgVedtak)

        assertThat(detaljer.unntaksvilkaarInnvilget).isFalse()
        assertThat(detaljer.unntaksvilkaarGjelderFra).isEqualTo(unntaksdato)
    }

    @Test
    fun `bruker stansvedtak når eget vedtak mangler komplette fakta`() {
        val vedtak = egetVedtak()
        val stans = stansvedtak()
        klargjørSak(listOf(vedtak, stans))
        every { vedtakfaktaService.hentForVedtakliste(listOf(stans.vedtakId)) } returns
            mapOf(stans.vedtakId to fakta("J"))

        for (ufullstendigeFakta in listOf(emptyList(), fakta("N").take(1), fakta("N").drop(1))) {
            every { vedtakfaktaService.hentForVedtakliste(listOf(vedtak.vedtakId)) } returns
                mapOf(vedtak.vedtakId to ufullstendigeFakta)

            val detaljer = requireNotNull(service.hentHistorikkStatusDetalj(personId).sisteSakOgVedtak)

            assertThat(detaljer.unntaksvilkaarInnvilget).isTrue()
            assertThat(detaljer.unntaksvilkaarGjelderFra).isEqualTo(unntaksdato)
        }
    }

    @Test
    fun `bruker iverksatte og avsluttede vedtak med gyldig eller manglende sluttdato`() {
        for (grunnlag in listOf(egetVedtak(), stansvedtak())) {
            for (status in listOf("IVERK", "AVSLU")) {
                for (sluttdato in listOf(unntaksdato, unntaksdato.plusDays(1), null)) {
                    val vedtak = grunnlag.copy(statusKode = status, tilDato = sluttdato)
                    klargjørSak(listOf(vedtak))
                    every { vedtakfaktaService.hentForVedtakliste(listOf(vedtak.vedtakId)) } returns
                        mapOf(vedtak.vedtakId to fakta("J"))

                    val detaljer = requireNotNull(service.hentHistorikkStatusDetalj(personId).sisteSakOgVedtak)

                    assertThat(detaljer.unntaksvilkaarInnvilget)
                        .describedAs("${vedtak.vedtaktypeKode}/$status/$sluttdato").isTrue()
                    assertThat(detaljer.unntaksvilkaarGjelderFra)
                        .describedAs("${vedtak.vedtaktypeKode}/$status/$sluttdato").isEqualTo(unntaksdato)
                }
            }
        }
    }

    @Test
    fun `filtrerer bort egne vedtak og stansvedtak som ikke oppfyller alle kriteriene`() {
        for (vedtak in listOf(egetVedtak(), stansvedtak())) {
            val ugyldigeVedtak = listOf(
                vedtak.copy(fraOgMed = null),
                vedtak.copy(fraOgMed = sluttdato.minusDays(1)),
                vedtak.copy(fraOgMed = sluttdato),
                vedtak.copy(vedtaktypeKode = if (vedtak.vedtaktypeKode == "O") "S" else "O"),
                vedtak.copy(vedtaktypeKode = "E"),
                vedtak.copy(rettighetkode = "DAGP"),
                vedtak.copy(utfallkode = if (vedtak.utfallkode == "JA") "NEI" else "JA"),
                vedtak.copy(utfallkode = null),
                vedtak.copy(aktivitetsfaseKode = "AKT"),
                vedtak.copy(statusKode = "REGIS"),
                vedtak.copy(statusKode = "KONT"),
                vedtak.copy(statusKode = "INNST"),
                vedtak.copy(tilDato = unntaksdato.minusDays(1)),
            )

            for ((indeks, ugyldigVedtak) in ugyldigeVedtak.withIndex()) {
                klargjørSak(listOf(ordinærtVedtak(), ugyldigVedtak))

                val detaljer = requireNotNull(service.hentHistorikkStatusDetalj(personId).sisteSakOgVedtak)

                assertThat(detaljer.unntaksvilkaarInnvilget)
                    .describedAs("${vedtak.vedtaktypeKode}/$indeks").isNull()
                assertThat(detaljer.unntaksvilkaarGjelderFra)
                    .describedAs("${vedtak.vedtaktypeKode}/$indeks").isNull()
            }
        }
        verify(exactly = 0) { vedtakfaktaService.hentForVedtakliste(match { it.isNotEmpty() }) }
        assertThat(logg.list).isEmpty()
    }

    @Test
    fun `velger nyeste komplette fakta uavhengig av rekkefølgen på vedtak og fakta`() {
        for (eldre in listOf(egetVedtak(), stansvedtak())) {
            val nyere = eldre.copy(vedtakId = 4, fraOgMed = unntaksdato.plusDays(10))

            for (faktaene in listOf(
                mapOf(eldre.vedtakId to fakta("J"), nyere.vedtakId to fakta("N")),
                mapOf(nyere.vedtakId to fakta("N"), eldre.vedtakId to fakta("J")),
            )) {
                every { vedtakfaktaService.hentForVedtakliste(listOf(nyere.vedtakId, eldre.vedtakId)) } returns faktaene

                for (vedtakene in listOf(listOf(eldre, nyere), listOf(nyere, eldre))) {
                    logg.list.clear()
                    klargjørSak(vedtakene)

                    val detaljer = requireNotNull(service.hentHistorikkStatusDetalj(personId).sisteSakOgVedtak)

                    assertThat(detaljer.unntaksvilkaarInnvilget).isFalse()
                    assertThat(detaljer.unntaksvilkaarGjelderFra).isEqualTo(unntaksdato)
                    assertThat(logg.list).hasSize(1)
                    assertThat(logg.list.single().level).isEqualTo(Level.WARN)
                    assertThat(logg.list.single().formattedMessage).isEqualTo(
                        "Fant flere aktuelle vedtak med vedtakfakta for 11-12 for sak 42. Bruker den første."
                    )
                }
            }
            verify(exactly = 4) {
                vedtakfaktaService.hentForVedtakliste(listOf(nyere.vedtakId, eldre.vedtakId))
            }
        }
    }

    @Test
    fun `logger ikke advarsel uten relevante vedtak eller med ett komplett faktasett`() {
        val vedtak = egetVedtak()
        every { vedtakfaktaService.hentForVedtakliste(listOf(vedtak.vedtakId)) } returns
            mapOf(vedtak.vedtakId to fakta("J"))

        for (vedtakene in listOf(listOf(ordinærtVedtak()), listOf(vedtak))) {
            klargjørSak(vedtakene)

            service.hentHistorikkStatusDetalj(personId)

            assertThat(logg.list).isEmpty()
        }
    }

    @Test
    fun `bruker eldre vedtak når nyeste vedtak mangler komplette fakta`() {
        for (eldre in listOf(egetVedtak(), stansvedtak())) {
            val nyere = eldre.copy(vedtakId = 4, fraOgMed = unntaksdato.plusDays(10))
            klargjørSak(listOf(eldre, nyere))

            for (ufullstendigeFakta in listOf(emptyList(), fakta("N").take(1), fakta("N").drop(1))) {
                every { vedtakfaktaService.hentForVedtakliste(listOf(nyere.vedtakId, eldre.vedtakId)) } returns
                    mapOf(nyere.vedtakId to ufullstendigeFakta, eldre.vedtakId to fakta("J"))

                val detaljer = requireNotNull(service.hentHistorikkStatusDetalj(personId).sisteSakOgVedtak)

                assertThat(detaljer.unntaksvilkaarInnvilget).isTrue()
                assertThat(detaljer.unntaksvilkaarGjelderFra).isEqualTo(unntaksdato)
            }
        }
    }

    @Test
    fun `manglende fakta gir ukjent status og advarsel`() {
        val vedtak = egetVedtak()
        klargjørSak(listOf(vedtak))
        every { vedtakfaktaService.hentForVedtakliste(listOf(vedtak.vedtakId)) } returns emptyMap()

        val detaljer = requireNotNull(service.hentHistorikkStatusDetalj(personId).sisteSakOgVedtak)

        assertThat(detaljer.unntaksvilkaarInnvilget).isNull()
        assertThat(detaljer.unntaksvilkaarGjelderFra).isNull()
        assertThat(logg.list).hasSize(1)
        assertThat(logg.list.single().level).isEqualTo(Level.WARN)
        assertThat(logg.list.single().formattedMessage).isEqualTo(
            "Fant relevante vedtak for 11-12 for sak 42 , men ingen vedtakfakta. Dette kan være en feil i Arena."
        )
    }

    @Test
    fun `manglende enkeltfakta gir ukjent status og advarsler`() {
        val vedtak = egetVedtak()
        klargjørSak(listOf(vedtak))

        for (kode in listOf("UNNTAKAAP", "AAPVILKUNN")) {
            logg.list.clear()
            every { vedtakfaktaService.hentForVedtakliste(listOf(vedtak.vedtakId)) } returns
                mapOf(vedtak.vedtakId to fakta("J").filter { it.kode == kode })

            val detaljer = requireNotNull(service.hentHistorikkStatusDetalj(personId).sisteSakOgVedtak)

            assertThat(detaljer.unntaksvilkaarInnvilget).describedAs(kode).isNull()
            assertThat(detaljer.unntaksvilkaarGjelderFra).describedAs(kode).isNull()
            assertThat(logg.list.map { it.level }).containsExactly(Level.WARN, Level.WARN)
            assertThat(logg.list.map { it.formattedMessage }).containsExactly(
                "Fant kun en av vedtakfakta-verdiene for 11-12 for vedtakId=2. Dette kan være en feil i Arena.",
                "Fant relevante vedtak for 11-12 for sak 42 , men ingen vedtakfakta. Dette kan være en feil i Arena.",
            )
        }
    }

    @Test
    fun `historikkstatus avviser tom vedtaksliste`() {
        klargjørSak(emptyList())

        assertThatIllegalArgumentException().isThrownBy {
            service.hentHistorikkStatusDetalj(personId)
        }.withMessage("SisteSakDetaljer må logisk sett inneholde minst ett vedtak")

        verify(exactly = 0) { vedtakfaktaService.hentForVedtakliste(match { it.isNotEmpty() }) }
        assertThat(logg.list).isEmpty()
    }

    @Test
    fun `historikkstatus avviser sak der alle vedtak er ugyldiggjort`() {
        klargjørSak(listOf(egetVedtak().copy(tilDato = unntaksdato.minusDays(1))))

        assertThatIllegalArgumentException().isThrownBy {
            service.hentHistorikkStatusDetalj(personId)
        }.withMessage("SisteSakDetaljer må logisk sett inneholde minst ett vedtak")
    }

    @Test
    fun `returnerer utvidet kvote uavhengig av unntaksstatus`() {
        every { telleverkService.hentTelleverkForPerson(personId) } returns TelleverkForPerson(100, 25)

        for (innvilget in listOf(true, false, null)) {
            klargjørSak(løpende = løpendeVedtak().copy(unntaksvilkaarInnvilget = innvilget))

            val telleverk = requireNotNull(service.hentHistorikkStatusDetalj(personId).telleverk)

            assertThat(telleverk.ordineerAAPKvote).isEqualTo(100)
            assertThat(telleverk.utvidetAAPKvote).isEqualTo(25)
        }
    }

    @Test
    fun `korrigerer negative kvoter til null dager`() {
        every { telleverkService.hentTelleverkForPerson(personId) } returns TelleverkForPerson(-100, -25)

        val telleverk = requireNotNull(service.hentHistorikkStatusDetalj(personId).telleverk)

        assertThat(telleverk.ordineerAAPKvote).isZero()
        assertThat(telleverk.utvidetAAPKvote).isZero()
    }

    @Test
    fun `bevarer manglende utvidet kvote`() {
        every { telleverkService.hentTelleverkForPerson(personId) } returns TelleverkForPerson(100, null)

        val telleverk = requireNotNull(service.hentHistorikkStatusDetalj(personId).telleverk)

        assertThat(telleverk.ordineerAAPKvote).isEqualTo(100)
        assertThat(telleverk.utvidetAAPKvote).isNull()
    }

    @Test
    fun `manglende løpende vedtak beholder telleverk og siste utbetaling`() {
        every { vedtakService.hentSisteLøpendeVedtakDetalj(personId) } returns null
        every { telleverkService.hentTelleverkForPerson(personId) } returns TelleverkForPerson(100, 25)
        every { utbetalingService.hentSisteAapUtbetalingForPerson(personId) } returns sluttdato

        val respons = service.hentHistorikkStatusDetalj(personId)

        assertThat(respons.sisteSakOgVedtak).isNull()
        assertThat(respons.telleverk?.ordineerAAPKvote).isEqualTo(100)
        assertThat(respons.telleverk?.utvidetAAPKvote).isEqualTo(25)
        assertThat(respons.sisteUtbetalingDato).isEqualTo(sluttdato)

        verify(exactly = 0) { sakOgVedtakService.hentSakMedVedtak(any()) }
        verify(exactly = 0) { vedtakfaktaService.hentForVedtakliste(any()) }
        verify(exactly = 1) { telleverkService.hentTelleverkForPerson(personId) }
        verify(exactly = 1) { utbetalingService.hentSisteAapUtbetalingForPerson(personId) }
    }

    @Test
    fun `manglende sak beholder telleverk og siste utbetaling`() {
        every { sakOgVedtakService.hentSakMedVedtak(any()) } returns null
        every { telleverkService.hentTelleverkForPerson(personId) } returns TelleverkForPerson(100, 25)
        every { utbetalingService.hentSisteAapUtbetalingForPerson(personId) } returns sluttdato

        val respons = service.hentHistorikkStatusDetalj(personId)

        assertThat(respons.sisteSakOgVedtak).isNull()
        assertThat(respons.telleverk?.ordineerAAPKvote).isEqualTo(100)
        assertThat(respons.telleverk?.utvidetAAPKvote).isEqualTo(25)
        assertThat(respons.sisteUtbetalingDato).isEqualTo(sluttdato)

        verify(exactly = 0) { vedtakfaktaService.hentForVedtakliste(any()) }
        verify(exactly = 1) { telleverkService.hentTelleverkForPerson(personId) }
        verify(exactly = 1) { utbetalingService.hentSisteAapUtbetalingForPerson(personId) }
    }

    @Test
    fun `manglende vedtak og telleverk beholder siste utbetaling`() {
        every { vedtakService.hentSisteLøpendeVedtakDetalj(personId) } returns null
        every { utbetalingService.hentSisteAapUtbetalingForPerson(personId) } returns sluttdato

        val respons = service.hentHistorikkStatusDetalj(personId)

        assertThat(respons.sisteSakOgVedtak).isNull()
        assertThat(respons.telleverk).isNull()
        assertThat(respons.sisteUtbetalingDato).isEqualTo(sluttdato)
    }

    @Test
    fun `manglende vedtak, telleverk og utbetaling gir tom historikkstatus`() {
        every { vedtakService.hentSisteLøpendeVedtakDetalj(personId) } returns null

        assertThat(service.hentHistorikkStatusDetalj(personId)).isEqualTo(HistorikkStatusDetaljResponse.ingen)

        verify(exactly = 1) { telleverkService.hentTelleverkForPerson(personId) }
        verify(exactly = 1) { utbetalingService.hentSisteAapUtbetalingForPerson(personId) }
    }

    private fun klargjørSak(
        vedtakene: List<ArenaVedtakMedDetaljer> = listOf(ordinærtVedtak()),
        løpende: ArenaVedtakMedMaxdato = løpendeVedtak(),
    ) {
        every { vedtakService.hentSisteLøpendeVedtakDetalj(personId) } returns løpende
        every { sakOgVedtakService.hentSakMedVedtak(saksnummer) } returns ArenaSakMedVedtak(
            sakId = "42",
            opprettetAar = saksnummer.aar,
            lopenr = saksnummer.lopenummerSak,
            person = ArenaSakPerson(personId.id, "testperson", "Test", "Person"),
            statuskode = "AKTIV",
            statusnavn = "Aktiv",
            registrertDato = sluttdato.atStartOfDay(),
            avsluttetDato = null,
            vedtak = vedtakene,
        )
    }

    private fun ordinærtVedtak() = egetVedtak().copy(
        vedtakId = 1,
        fraOgMed = sluttdato.minusMonths(1),
        tilDato = sluttdato,
        utfallkode = "JA",
        aktivitetsfaseKode = "AAP",
    )

    private fun løpendeVedtak() = ArenaVedtakMedMaxdato(
        sakId = 42,
        vedtakId = 1,
        opprettetAar = 2026,
        lopenrVedtak = 1,
        lopenrSak = 17,
        statusKode = "IVERK",
        vedtaktypeKode = "O",
        aktivitetsfaseKode = "AKT",
        fraOgMed = sluttdato.minusMonths(1),
        tilDato = sluttdato,
        rettighetkode = "AAP",
        utfallkode = "JA",
        maxdatoUnntak = null,
        maxdatoOrdinaer = sluttdato,
    )

    private fun egetVedtak() = ArenaVedtakMedDetaljer(
        vedtakId = 2,
        lopenrvedtak = 2,
        statusKode = "IVERK",
        statusNavn = "Iverksatt",
        vedtaktypeKode = "O",
        vedtaktypeNavn = "Ordinært",
        aktivitetsfaseKode = "AU",
        aktivitetsfaseNavn = "AU",
        fraOgMed = unntaksdato,
        tilDato = unntaksdato.plusMonths(1),
        rettighetkode = "AAP",
        rettighetnavn = "AAP",
        utfallkode = "NEI",
        begrunnelse = null,
        saksbehandler = null,
        beslutter = null,
        relatertVedtak = null,
        fakta = emptyList(),
    )

    private fun stansvedtak() = egetVedtak().copy(
        vedtakId = 3,
        vedtaktypeKode = "S",
        vedtaktypeNavn = "Stans",
        utfallkode = "JA",
    )

    private fun fakta(innvilget: String) = listOf(
        ArenaVedtakfakta("UNNTAKAAP", "Unntak", innvilget, unntaksdato),
        ArenaVedtakfakta("AAPVILKUNN", "Unntaksdato", "01-02-2026", unntaksdato),
    )
}
