package no.nav.aap.arenaoppslag.modeller

import no.nav.aap.arenaoppslag.kontrakt.intern.Kilde
import no.nav.aap.arenaoppslag.kontrakt.intern.Status
import no.nav.aap.arenaoppslag.kontrakt.modeller.Periode
import java.time.LocalDate

data class VedtakStatus(
    val sakId: String,
    val statusKode: Status,
    val periode: Periode,
    val kilde: Kilde = Kilde.ARENA
)

data class ArenaVedtak(
    val sakId: String,
    val statusKode: String,
    val vedtaktypeKode: String?,
    val fraOgMed: LocalDate?,
    val tilDato: LocalDate?,
    val rettighetkode: String,
    val utfallkode: String?
)

data class ArenaVedtakUtenFakta(
    val vedtakId: Int,
    val statusKode: String,
    val statusNavn: String,
    val vedtaktypeKode: String,
    val vedtaktypeNavn: String,
    val aktivitetsfaseKode: String,
    val aktivitetsfaseNavn: String,
    val fraOgMed: LocalDate?,
    val tilDato: LocalDate?,
    val rettighetkode: String,
    val utfallkode: String?,
) {
    fun medFakta(fakta: List<ArenaVedtakfakta>) = ArenaVedtakMedFakta(
        vedtakId = vedtakId,
        statusKode = statusKode,
        statusNavn = statusNavn,
        vedtaktypeKode = vedtaktypeKode,
        vedtaktypeNavn = vedtaktypeNavn,
        aktivitetsfaseKode = aktivitetsfaseKode,
        aktivitetsfaseNavn = aktivitetsfaseNavn,
        fraOgMed = fraOgMed,
        tilDato = tilDato,
        rettighetkode = rettighetkode,
        utfallkode = utfallkode,
        fakta = fakta,
    )
}

data class ArenaVedtakMedFakta(
    val vedtakId: Int,
    val statusKode: String,
    val statusNavn: String,
    val vedtaktypeKode: String,
    val vedtaktypeNavn: String,
    val aktivitetsfaseKode: String,
    val aktivitetsfaseNavn: String,
    val fraOgMed: LocalDate?,
    val tilDato: LocalDate?,
    val rettighetkode: String,
    val utfallkode: String?,
    val fakta: List<ArenaVedtakfakta>
)

data class ArenaVedtakfakta(
    val kode: String,
    val navn: String,
    val verdi: String?,
    val registrertDato: LocalDate,
)