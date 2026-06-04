package no.nav.aap.arenaoppslag.database

import no.nav.aap.arenaoppslag.kontrakt.intern.Status
import no.nav.aap.arenaoppslag.modeller.ArenaVedtakRad
import no.nav.aap.arenaoppslag.modeller.PersonId
import no.nav.aap.arenaoppslag.modeller.SakId
import no.nav.aap.arenaoppslag.modeller.VedtakStatus
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple.tuple
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import no.nav.aap.arenaoppslag.kontrakt.modeller.Periode as KontraktPeriode

class VedtakRepositoryTest : H2TestBase("flyway/minimumtest") {

    @Test
    fun `hente aktfasePerioder`() {
        val forventetVedtaksperioder = listOf(
            VedtakStatus(
                sakId = "4", Status.IVERK,
                KontraktPeriode(
                    LocalDate.of(2022, 8, 30),
                    null
                )
            )
        )
        val vedtakRepository = VedtakRepository(h2)
        val alleVedtak = vedtakRepository.hentVedtakStatuser(
            fodselsnr = "nulltildato",
        )

        assertEquals(forventetVedtaksperioder, alleVedtak)
    }

    @Test
    fun `hentVedtakForSak klarer å hente alle vedtak fra databasen`() {
        val vedtakRepository = VedtakRepository(h2)
        val forventetVedtak = ArenaVedtakRad(
            vedtakId = 1234,
            lopenrvedtak = 1,
            statusKode = "IVERK",
            statusNavn = "Iverksatt",
            vedtaktypeKode = "O",
            vedtaktypeNavn = "Ny rettighet",
            aktivitetsfaseKode = "IKKE",
            aktivitetsfaseNavn = "Ikke spesif. aktivitetsfase",
            fraOgMed = LocalDate.of(2022, 8, 30),
            tilDato = LocalDate.of(2023, 8, 30),
            rettighetkode = "AAP",
            rettighetnavn = "Arbeidsavklaringspenger",
            utfallkode = "JA",
            begrunnelse = null,
            saksbehandler = null,
            beslutter = null,
            relatertVedtak = null,
        )

        val alleVedtak = vedtakRepository.hentVedtakForSak(SakId(1))

        assertThat(alleVedtak).containsExactly(forventetVedtak)
    }

    @Test
    fun `hentVedtakForSak returnerer tom liste om det ikke er noen vedtak knyttet til denne saken`() {
        val vedtakRepository = VedtakRepository(h2)
        val alleVedtak = vedtakRepository.hentVedtakForSak(SakId(1919191919))
        assertThat(alleVedtak).isEmpty()
    }

    @Test
    fun `hentVedtakForSak returnerer vedtak med relatertVedtak satt`() {
        val vedtakRepository = VedtakRepository(h2)
        val vedtak = vedtakRepository.hentVedtakForSak(SakId(9))

        assertThat(vedtak).hasSize(1)
        assertThat(vedtak.single().relatertVedtak).isEqualTo(1234)
    }

    @Test
    fun `hentVedtak for person returnerer forventet vedtak`() {
        val vedtakRepository = VedtakRepository(h2)

        val vedtak = vedtakRepository.hentVedtak(PersonId(1))
        val enesteVedtak = vedtak.single()

        assertThat(enesteVedtak.sakId).isEqualTo("1")
        assertThat(enesteVedtak.statusKode).isEqualTo("IVERK")
        assertThat(enesteVedtak.vedtaktypeKode).isEqualTo("O")
        assertThat(enesteVedtak.fraOgMed).isEqualTo(LocalDate.of(2022, 8, 30))
        assertThat(enesteVedtak.tilDato).isEqualTo(LocalDate.of(2023, 8, 30))
        assertThat(enesteVedtak.rettighetkode).isEqualTo("AAP")
        assertThat(enesteVedtak.utfallkode).isEqualTo("JA")
    }

    @Test
    fun `hentVedtak for person returnerer tom liste når person ikke har vedtak`() {
        val vedtakRepository = VedtakRepository(h2)

        val vedtak = vedtakRepository.hentVedtak(PersonId(3))

        assertThat(vedtak).isEmpty()
    }

    @Test
    fun `hentVedtak for person returnerer alle vedtak for person uten filtrering`() {
        val vedtakRepository = VedtakRepository(h2)

        val vedtak = vedtakRepository.hentVedtak(PersonId(5))

        assertThat(vedtak).hasSize(5)
        assertThat(vedtak).extracting(
            "statusKode",
            "vedtaktypeKode",
            "rettighetkode",
            "utfallkode"
        ).containsExactlyInAnyOrder(
            tuple("KONT", "O", "AAP", "JA"),
            tuple("IVERK", "N", "AAP", "JA"),
            tuple("IVERK", "O", "AAP", "NEI"),
            tuple("IVERK", "O", "AAP", "JA"),
            tuple("IVERK", "O", "OOP", "JA"),
        )
    }
}

