package no.nav.aap.arenaoppslag.kontrakt.apiv1

import java.time.LocalDate
import java.time.format.DateTimeFormatter

public data class VedtakForPersonRequest(val personidentifikator: String)

public data class ArenaVedtak(
    val sakId: Int,
    val statusKode: String,
    val vedtaktypeKode: String?,
    val fraOgMed: LocalDate?,
    val tilDato: LocalDate?,
    val rettighetkode: String,
    val utfallkode: String?,
    val vedtakId: Int,
    val aktivitetsfaseKode: String,
) {
    public fun erStans(): Boolean {
        return vedtaktypeKode == "S"
    }
}

@Deprecated("bruk nytt navn uten -Kontrakt suffiks", level = DeprecationLevel.ERROR)
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

@Deprecated("bruk nytt navn uten -Kontrakt suffiks", level = DeprecationLevel.ERROR)
public typealias ArenaVedtakfaktaKontrakt = ArenaVedtakfakta

public data class ArenaVedtakfakta(
    val kode: String,
    val navn: String,
    val verdi: String?,
    val registrertDato: LocalDate,
) {

    public companion object {
        private val DATO_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")

        private fun parseDato(verdi: String?): LocalDate? =
            verdi?.takeIf { it.isNotBlank() }?.let { LocalDate.parse(it, DATO_FORMAT) }
    }

    public fun somDatoVerdi() = verdi?.let { parseDato(it) }

    public fun somBooleanVerdi() = when (verdi) {
        "J" -> true
        "N" -> false
        else -> null
    }
}

@Deprecated("bruk nytt navn uten -Kontrakt suffiks", level = DeprecationLevel.ERROR)
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
