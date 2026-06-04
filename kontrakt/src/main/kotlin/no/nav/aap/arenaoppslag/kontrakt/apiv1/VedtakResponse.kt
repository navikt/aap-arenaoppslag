package no.nav.aap.arenaoppslag.kontrakt.apiv1

import java.time.LocalDate

public data class VedtakForPersonRequest(val personidentifikator: String)

@Deprecated("bruk nytt navn uten -Kontrakt suffiks", level= DeprecationLevel.ERROR)
public typealias ArenaVedtakMedDetaljerKontrakt = ArenaVedtakMedDetaljer

public data class ArenaVedtakMedDetaljer(
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
    val fakta: List<ArenaVedtakfakta>
)

@Deprecated("bruk nytt navn uten -Kontrakt suffiks", level= DeprecationLevel.ERROR)
public typealias ArenaVedtakfaktaKontrakt = ArenaVedtakfakta

public data class ArenaVedtakfakta(
    val kode: String,
    val navn: String,
    val verdi: String?,
    val registrertDato: LocalDate,
)

@Deprecated("bruk nytt navn uten -Kontrakt suffiks", level= DeprecationLevel.ERROR)
public typealias ArenaVilkårsvurderingKontrakt = ArenaVilkårsvurdering

public data class ArenaVilkårsvurdering(
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
