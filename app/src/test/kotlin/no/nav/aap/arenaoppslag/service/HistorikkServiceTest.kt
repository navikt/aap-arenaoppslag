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
import org.assertj.core.api.Assertions.assertThat
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
    private val sluttdato = LocalDate.of(2026, 1, 31)
    private val unntaksdato = sluttdato.plusDays(1)
    private val logger = LoggerFactory.getLogger(HistorikkService::class.java) as Logger
    private val opprinneligLoggnivå = logger.level
    private val logg = ListAppender<ILoggingEvent>()

    @BeforeEach
    fun setup() {
        every { utbetalingService.hentSisteAapUtbetalingForPerson(personId) } returns null
        every { telleverkService.hentTelleverkForPerson(personId) } returns null
        klargjørSak(emptyList())
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
    fun `gammel prosedyre har forrang foran fakta fra eget vedtak`() {
        val vedtak = egetVedtak()
        every { vedtakfaktaService.hentForVedtakliste(listOf(vedtak.vedtakId)) } returns
            mapOf(vedtak.vedtakId to fakta("J"))

        for (innvilget in listOf(true, false)) {
            klargjørSak(
                listOf(vedtak),
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
    fun `filtrerer bort vedtak som ikke oppfyller alle kriteriene`() {
        val vedtak = egetVedtak()
        val ugyldigeVedtak = listOf(
            vedtak.copy(fraOgMed = null),
            vedtak.copy(fraOgMed = sluttdato.minusDays(1)),
            vedtak.copy(fraOgMed = sluttdato),
            vedtak.copy(vedtaktypeKode = "S"),
            vedtak.copy(rettighetkode = "DAGP"),
            vedtak.copy(utfallkode = "JA"),
            vedtak.copy(utfallkode = null),
            vedtak.copy(aktivitetsfaseKode = "AKT"),
        )

        for ((indeks, ugyldigVedtak) in ugyldigeVedtak.withIndex()) {
            klargjørSak(listOf(ugyldigVedtak))

            val detaljer = requireNotNull(service.hentHistorikkStatusDetalj(personId).sisteSakOgVedtak)

            assertThat(detaljer.unntaksvilkaarInnvilget).describedAs("Tilfelle $indeks").isNull()
            assertThat(detaljer.unntaksvilkaarGjelderFra).describedAs("Tilfelle $indeks").isNull()
        }
        verify(exactly = 0) { vedtakfaktaService.hentForVedtakliste(any()) }
        assertThat(logg.list).isEmpty()
    }

    @Test
    fun `velger nyeste vedtak uavhengig av rekkefølge og logger en advarsel`() {
        val eldre = egetVedtak()
        val nyere = eldre.copy(vedtakId = 3, fraOgMed = unntaksdato.plusDays(10))
        every { vedtakfaktaService.hentForVedtakliste(listOf(nyere.vedtakId)) } returns
            mapOf(nyere.vedtakId to fakta("N"))

        for (vedtakene in listOf(listOf(eldre, nyere), listOf(nyere, eldre))) {
            logg.list.clear()
            klargjørSak(vedtakene)

            val detaljer = requireNotNull(service.hentHistorikkStatusDetalj(personId).sisteSakOgVedtak)

            assertThat(detaljer.unntaksvilkaarInnvilget).isFalse()
            assertThat(detaljer.unntaksvilkaarGjelderFra).isEqualTo(unntaksdato)
            assertThat(logg.list).hasSize(1)
            assertThat(logg.list.single().level).isEqualTo(Level.WARN)
            assertThat(logg.list.single().formattedMessage).isEqualTo(
                "Fant 2 egne 11-12-vedtak i siste sak. Bruker vedtaket med nyeste fra-og-med-dato."
            )
        }
        verify(exactly = 0) { vedtakfaktaService.hentForVedtakliste(listOf(eldre.vedtakId)) }
    }

    @Test
    fun `logger ikke advarsel med null eller ett eget vedtak`() {
        val vedtak = egetVedtak()
        every { vedtakfaktaService.hentForVedtakliste(listOf(vedtak.vedtakId)) } returns emptyMap()

        for (vedtakene in listOf(emptyList(), listOf(vedtak))) {
            klargjørSak(vedtakene)

            service.hentHistorikkStatusDetalj(personId)

            assertThat(logg.list).isEmpty()
        }
    }

    @Test
    fun `manglende fakta gir ukjent status uten å bruke eldre vedtak`() {
        val eldre = egetVedtak()
        val nyere = eldre.copy(vedtakId = 3, fraOgMed = unntaksdato.plusDays(10))
        klargjørSak(listOf(eldre, nyere))
        every { vedtakfaktaService.hentForVedtakliste(listOf(nyere.vedtakId)) } returns emptyMap()

        val detaljer = requireNotNull(service.hentHistorikkStatusDetalj(personId).sisteSakOgVedtak)

        assertThat(detaljer.unntaksvilkaarInnvilget).isNull()
        assertThat(detaljer.unntaksvilkaarGjelderFra).isNull()
        verify(exactly = 0) { vedtakfaktaService.hentForVedtakliste(listOf(eldre.vedtakId)) }
    }

    @Test
    fun `manglende enkeltfakta gir null bare for det aktuelle feltet`() {
        val vedtak = egetVedtak()
        klargjørSak(listOf(vedtak))
        every { vedtakfaktaService.hentForVedtakliste(listOf(vedtak.vedtakId)) } returns
            mapOf(vedtak.vedtakId to fakta("J").filter { it.kode == "UNNTAKAAP" })

        val utenDato = requireNotNull(service.hentHistorikkStatusDetalj(personId).sisteSakOgVedtak)

        assertThat(utenDato.unntaksvilkaarInnvilget).isTrue()
        assertThat(utenDato.unntaksvilkaarGjelderFra).isNull()

        every { vedtakfaktaService.hentForVedtakliste(listOf(vedtak.vedtakId)) } returns
            mapOf(vedtak.vedtakId to fakta("J").filter { it.kode == "AAPVILKUNN" })

        val utenStatus = requireNotNull(service.hentHistorikkStatusDetalj(personId).sisteSakOgVedtak)

        assertThat(utenStatus.unntaksvilkaarInnvilget).isNull()
        assertThat(utenStatus.unntaksvilkaarGjelderFra).isEqualTo(unntaksdato)
    }

    @Test
    fun `ingen vedtak gir ukjent 11-12-status`() {
        val detaljer = requireNotNull(service.hentHistorikkStatusDetalj(personId).sisteSakOgVedtak)

        assertThat(detaljer.unntaksvilkaarInnvilget).isNull()
        assertThat(detaljer.unntaksvilkaarGjelderFra).isNull()
        verify(exactly = 0) { vedtakfaktaService.hentForVedtakliste(any()) }
    }

    @Test
    fun `manglende løpende vedtak gir tom historikkstatus`() {
        every { vedtakService.hentSisteLøpendeVedtakDetalj(personId) } returns null

        assertThat(service.hentHistorikkStatusDetalj(personId)).isEqualTo(HistorikkStatusDetaljResponse.ingen)

        verify(exactly = 0) { sakOgVedtakService.hentSakMedVedtak(any()) }
        verify(exactly = 0) { vedtakfaktaService.hentForVedtakliste(any()) }
    }

    @Test
    fun `manglende sak gir tom historikkstatus`() {
        every { sakOgVedtakService.hentSakMedVedtak(any()) } returns null

        assertThat(service.hentHistorikkStatusDetalj(personId)).isEqualTo(HistorikkStatusDetaljResponse.ingen)

        verify(exactly = 0) { vedtakfaktaService.hentForVedtakliste(any()) }
    }

    private fun klargjørSak(
        vedtakene: List<ArenaVedtakMedDetaljer>,
        løpende: ArenaVedtakMedMaxdato = løpendeVedtak(),
    ) {
        every { vedtakService.hentSisteLøpendeVedtakDetalj(personId) } returns løpende
        every { sakOgVedtakService.hentSakMedVedtak(løpende.saksnummer()) } returns ArenaSakMedVedtak(
            sakId = "42",
            opprettetAar = 2026,
            lopenr = 1,
            person = ArenaSakPerson(personId.id, "testperson", "Test", "Person"),
            statuskode = "AKTIV",
            statusnavn = "Aktiv",
            registrertDato = sluttdato.atStartOfDay(),
            avsluttetDato = null,
            vedtak = vedtakene,
        )
    }

    private fun løpendeVedtak() = ArenaVedtakMedMaxdato(
        sakId = 42,
        vedtakId = 1,
        opprettetAar = 2026,
        lopenrvedtak = 1,
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
        tilDato = null,
        rettighetkode = "AAP",
        rettighetnavn = "AAP",
        utfallkode = "NEI",
        begrunnelse = null,
        saksbehandler = null,
        beslutter = null,
        relatertVedtak = null,
        fakta = emptyList(),
    )

    private fun fakta(innvilget: String) = listOf(
        ArenaVedtakfakta("UNNTAKAAP", "Unntak", innvilget, unntaksdato),
        ArenaVedtakfakta("AAPVILKUNN", "Unntaksdato", "01-02-2026", unntaksdato),
    )
}
