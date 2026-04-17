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
    val registrertDato: LocalDate?,
    val fraOgMed: LocalDate?,
    val tilDato: LocalDate?,
    val rettighetkode: String,
    val utfallkode: String?
)

data class ArenaVedtakRad(
    val vedtakId: Int,
    val lopenrvedtak: Int,
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
    fun medFakta(
        fakta: List<ArenaVedtakfakta>,
        vilkårsvurderinger: List<ArenaVilkårsvurdering> = emptyList(),
    ) = ArenaVedtakMedDetaljer(
        vedtakId = vedtakId,
        lopenrvedtak = lopenrvedtak,
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
        vilkårsvurderinger = vilkårsvurderinger,
    )
}

data class ArenaVedtakMedDetaljer(
    val vedtakId: Int,
    val lopenrvedtak: Int,
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
    val fakta: List<ArenaVedtakfakta>,
    val vilkårsvurderinger: List<ArenaVilkårsvurdering> = emptyList(),
)

data class ArenaVedtakfakta(
    val kode: String,
    val navn: String,
    val verdi: String?,
    val registrertDato: LocalDate,
)

data class ArenaVilkårsvurdering(
    val vilkårsvurderingId: Long,
    val vilkårkode: String,
    val begrunnelse: String?,
    val vurdertAv: String?,
    val vilkårnavn: String,
    val erObligatorisk: Boolean,
    val hjelpetekstUrl: String?,
    val lovtekstUrl: String?,
    val rundskrivUrl: String?,
    val statuskode: String,
    val statusnavn: String,
)
