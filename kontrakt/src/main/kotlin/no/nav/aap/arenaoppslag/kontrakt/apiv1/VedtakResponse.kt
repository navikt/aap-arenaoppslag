package no.nav.aap.arenaoppslag.kontrakt.apiv1

import java.time.LocalDate

public data class VedtakForPersonRequest(val personidentifikator: String)

public data class ArenaVedtakMedDetaljerKontrakt(
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
    val rettighetnavn: String,
    val utfallkode: String?,
    val begrunnelse: String?,
    val saksbehandler: String?,
    val beslutter: String?,
    val relatertVedtak: Int?,
    val fakta: List<ArenaVedtakfaktaKontrakt>,
    val vilkårsvurderinger: List<ArenaVilkårsvurderingKontrakt>,
)

public data class ArenaVedtakfaktaKontrakt(
    val kode: String,
    val navn: String,
    val verdi: String?,
    val registrertDato: LocalDate,
)

public data class ArenaVilkårsvurderingKontrakt(
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
