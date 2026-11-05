package no.nav.aap.arenaoppslag.modeller

import no.nav.aap.arenaoppslag.kontrakt.apiv1.SisteSakDetaljer
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test
import java.time.LocalDate
import no.nav.aap.arenaoppslag.kontrakt.apiv1.ArenaVedtak as ArenaVedtakKontrakt

class ArenaSakMedVedtakTest {
    private val fraOgMed = LocalDate.of(2025, 1, 1)
    private val tilDato = LocalDate.of(2025, 12, 31)
    private val sisteVedtak = ArenaVedtakMedMaxdato(
        sakId = 42,
        vedtakId = 101,
        opprettetAar = 2025,
        lopenrVedtak = 3,
        lopenrSak = 7,
        statusKode = "IVERK",
        vedtaktypeKode = "O",
        aktivitetsfaseKode = "UA",
        fraOgMed = fraOgMed,
        tilDato = tilDato,
        rettighetkode = "AAP",
        utfallkode = "JA",
        maxdatoUnntak = null,
        maxdatoOrdinaer = tilDato,
    )
    private val vedtak = ArenaVedtakMedDetaljer(
        vedtakId = 101,
        lopenrvedtak = 3,
        statusKode = "IVERK",
        statusNavn = "Iverksatt",
        vedtaktypeKode = "O",
        vedtaktypeNavn = "Ny rettighet",
        aktivitetsfaseKode = "UA",
        aktivitetsfaseNavn = "Under avklaring",
        fraOgMed = fraOgMed,
        tilDato = tilDato,
        rettighetkode = "AAP",
        rettighetnavn = "Arbeidsavklaringspenger",
        utfallkode = "JA",
        begrunnelse = null,
        saksbehandler = null,
        beslutter = null,
        relatertVedtak = null,
        fakta = emptyList(),
    )
    private val sak = ArenaSakMedVedtak(
        sakId = "42",
        opprettetAar = 2025,
        lopenr = 7,
        person = ArenaSakPerson(1, "12312312312", "Test", "Testesen"),
        statuskode = "AKTIV",
        statusnavn = "Aktiv",
        registrertDato = fraOgMed.atTime(12, 30),
        avsluttetDato = tilDato.atTime(15, 45),
        vedtak = listOf(vedtak, vedtak.copy(vedtakId = 102, utfallkode = null)),
    )

    @Test
    fun `saksnummer bruker sakens løpenummer og ikke vedtakets`() {
        assertThat(sisteVedtak.saksnummer()).isEqualTo(Saksnummer(lopenummerSak = 7, aar = 2025))
    }

    @Test
    fun `toKontrakt velger maksdato for unntak foran ordinær maksdato uavhengig av unntaksdato`() {
        val maksdatoUnntak = tilDato.plusYears(1)
        val unntaksdato = fraOgMed.plusMonths(1)

        for ((unntak, ordinær, forventet) in listOf(
            Triple(maksdatoUnntak, tilDato, maksdatoUnntak),
            Triple(maksdatoUnntak, null, maksdatoUnntak),
            Triple(null, tilDato, tilDato),
            Triple(null, null, null),
        )) {
            for (innvilget in listOf(true, false, null)) {
                val kontrakt = sisteVedtak.copy(
                    maxdatoUnntak = unntak,
                    maxdatoOrdinaer = ordinær,
                    unntaksvilkaarInnvilget = innvilget,
                    unntaksvilkaarGjelderFra = unntaksdato,
                ).toKontrakt()

                assertThat(kontrakt.maxdatoAap).describedAs("$unntak/$ordinær/$innvilget").isEqualTo(forventet)
                assertThat(kontrakt.maxdatoUnntak).isEqualTo(unntak)
                assertThat(kontrakt.maxdatoOrdinaer).isEqualTo(ordinær)
            }
        }
    }

    @Test
    fun `tilSisteSakDetaljer mapper sak og vedtak med beregnet unntaksstatus`() {
        val unntaksdato = tilDato.plusDays(1)

        val detaljer = sak.tilSisteSakDetaljer(sisteVedtak, true, unntaksdato)

        val forventetVedtak = ArenaVedtakKontrakt(
            sakId = 42,
            vedtaktypeKode = "O",
            statusKode = "IVERK",
            tilDato = tilDato,
            fraOgMed = fraOgMed,
            rettighetkode = "AAP",
            utfallkode = "JA",
            vedtakId = 101,
            aktivitetsfaseKode = "UA",
        )
        assertThat(detaljer).isEqualTo(
            SisteSakDetaljer(
                sakId = 42,
                saknummer = "2025-7",
                sakStatus = "AKTIV",
                sakRegistrert = fraOgMed,
                sakAvsluttet = tilDato,
                unntaksvilkaarInnvilget = true,
                unntaksvilkaarGjelderFra = unntaksdato,
                sisteVedtak = sisteVedtak.toKontrakt(),
                vedtakene = listOf(forventetVedtak, forventetVedtak.copy(vedtakId = 102, utfallkode = null)),
            )
        )
    }

    @Test
    fun `tilSisteSakDetaljer bevarer nullverdier`() {
        val detaljer = sak.copy(avsluttetDato = null)
            .tilSisteSakDetaljer(sisteVedtak, null, null)

        assertThat(detaljer.sakAvsluttet).isNull()
        assertThat(detaljer.unntaksvilkaarInnvilget).isNull()
        assertThat(detaljer.unntaksvilkaarGjelderFra).isNull()
        assertThat(detaljer.vedtakene).hasSize(2)
    }

    @Test
    fun `tilSisteSakDetaljer avviser tom vedtaksliste`() {
        assertThatIllegalArgumentException().isThrownBy {
            sak.copy(vedtak = emptyList()).tilSisteSakDetaljer(sisteVedtak, null, null)
        }.withMessage("SisteSakDetaljer må logisk sett inneholde minst ett vedtak")
    }

    @Test
    fun `SisteSakDetaljer copy avviser tom vedtaksliste`() {
        val detaljer = sak.tilSisteSakDetaljer(sisteVedtak, null, null)

        assertThatIllegalArgumentException().isThrownBy {
            detaljer.copy(vedtakene = emptyList())
        }.withMessage("SisteSakDetaljer må logisk sett inneholde minst ett vedtak")
    }

    @Test
    fun `SisteSakDetaljer avviser vedtak fra en annen sak`() {
        val detaljer = sak.tilSisteSakDetaljer(sisteVedtak, null, null)

        assertThatIllegalArgumentException().isThrownBy {
            detaljer.copy(
                vedtakene = listOf(detaljer.vedtakene.first(), detaljer.vedtakene.last().copy(sakId = 43))
            )
        }.withMessage("Vedtakene må høre til samme sak")
    }
}
