package no.nav.aap.arenaoppslag.modeller

import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class ArenaSaksopplysning(
    val saksopplysningId: Long,
    val saksopplysningkode: String,
    val saksopplysningnavn: String,
    val skjermbildetekst: String?,
    val statusRepeterbar: String,
    val verdi: String?,
    val attributter: List<ArenaSaksopplysningAttributt>,
)

data class ArenaSaksopplysningAttributt(
    val attributtkode: String,
    val skjermbildetekst: String?,
    val formatnavn: String?,
    val posisjon: Int,
    val verdi: String?,
    val statusSjekketAv: String?,
)

data class InstitusjonOpphold(
    val type : InstitusjonOppholdType,
    val fra: LocalDate,
    val til: LocalDate?,
    val friKostOgLosji: Boolean,
    val reduksjonsType: ReduksjonType?,
) {
    companion object {
        const val SAKSOPPLYSNINGKODE = "INSOPPH"
        const val ATTRIBUTT_STRAFFEGJENNOMFORING = "STRFG"
        const val ATTRIBUTT_INSTA = "INSTA"
        const val ATTRIBUTT_FRA = "INFRA"
        const val ATTRIBUTT_TIL = "INTIL"
        const val ATTRIBUTT_FRI_KOST_LOSJI = "FRIKL"
        const val ATTRIBUTT_REDUKSJON = "REDPR"
    }
}

data class AnnenYtelse(
    val type: AnnenYtelseType,
    val belopPeriode: BelopPeriode?,
    val grad: String?,
    val beløp: String?,
) {
    companion object {
        const val SAKSOPPLYSNINGKODE = "AAOKYT"
        const val ATTRIBUTT_TYPE = "TYPE"
        const val ATTRIBUTT_GRAD = "GRAD"
        const val ATTRIBUTT_BELOP = "BELOP"
        const val ATTRIBUTT_BELOP_PERIODE = "BELPR"
    }
}

data class SamordningOgInstitusjon(
    val institusjonOpphold: List<InstitusjonOpphold>,
    val andreYtelser: List<AnnenYtelse>,
)

private val INSTITUSJON_DATO_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")

fun ArenaSaksopplysning.tilInstitusjonOpphold(): InstitusjonOpphold? {
    if (saksopplysningkode != InstitusjonOpphold.SAKSOPPLYSNINGKODE) return null
    fun attr(kode: String) = attributter.find { it.attributtkode == kode }?.verdi
    // Vises kun når straffegjennomføring (STRFG) eller institusjonsopphold (INSTA) er J
    val type = when {
        attr(InstitusjonOpphold.ATTRIBUTT_STRAFFEGJENNOMFORING) == "J" -> InstitusjonOppholdType.FENGSEL
        attr(InstitusjonOpphold.ATTRIBUTT_INSTA) == "J" -> InstitusjonOppholdType.Helseinstitusjon
        else -> return null
    }
    val fra = attr(InstitusjonOpphold.ATTRIBUTT_FRA)?.let { LocalDate.parse(it, INSTITUSJON_DATO_FORMAT) } ?: return null
    val til = attr(InstitusjonOpphold.ATTRIBUTT_TIL)?.let { LocalDate.parse(it, INSTITUSJON_DATO_FORMAT) }
    val friKostOgLosji = attr(InstitusjonOpphold.ATTRIBUTT_FRI_KOST_LOSJI) == "J"
    val reduksjonsType = attr(InstitusjonOpphold.ATTRIBUTT_REDUKSJON)?.let { ReduksjonType.fraKode(it) }
    return InstitusjonOpphold(type, fra, til, friKostOgLosji, reduksjonsType)
}

fun ArenaSaksopplysning.tilAnnenYtelse(): AnnenYtelse? {
    if (saksopplysningkode != AnnenYtelse.SAKSOPPLYSNINGKODE) return null
    fun attr(kode: String) = attributter.find { it.attributtkode == kode }?.verdi
    val type = attr(AnnenYtelse.ATTRIBUTT_TYPE)?.let { AnnenYtelseType.fraKode(it) } ?: return null
    val belopPeriode = attr(AnnenYtelse.ATTRIBUTT_BELOP_PERIODE)?.let { BelopPeriode.fraKode(it) }
    val grad = attr(AnnenYtelse.ATTRIBUTT_GRAD)
    val beløp = attr(AnnenYtelse.ATTRIBUTT_BELOP)
    return AnnenYtelse(type, belopPeriode, grad, beløp)
}

enum class InstitusjonOppholdType(val kode: String, val visningsnavn: String) {
    FENGSEL("FENGSEL", "Straffegjennomføring"),
    Helseinstitusjon("HELSEINS", "Helseinstitusjon");

    companion object {
        fun fraKode(kode: String): InstitusjonOppholdType? = entries.find { it.kode == kode }
    }
}

enum class ReduksjonType(val kode: String, val prosent: Int) {
    INGEN("RED00", 0),
    HALV( "RED50", 50);

    companion object {
        fun fraKode(kode: String): ReduksjonType? = entries.find { it.kode == kode }
    }
}

enum class AnnenYtelseType(val kode: String, val visningsnavn: String) {
    FORELDREPENGER_ADOPSJON("AP",   "Foreldrepenger adopsjon"),
    BARNEPENSJON(           "BP",   "Barnepensjon"),
    OMSORGSPENGER(          "BS",   "Omsorgspenger ved barns eller barnepassers sykdom"),
    FORELDREPENGER_FODSEL(  "FP",   "Foreldrepenger fødsel"),
    LONN_FRA_ARBEIDSGIVER(  "LØNN", "Økonomiske ytelser fra tidligere arbeidsgiver"),
    OPPLARINGSPENGER(       "OP",   "Opplæringspenger"),
    PLEIEPENGER(            "PB",   "Pleiepenger"),
    SVANGERSKAPSPENGER(     "SV",   "Svangerskapspenger"),
    UFORETRYGD(             "UP",   "Uføretrygd");

    companion object {
        fun fraKode(kode: String): AnnenYtelseType? = entries.find { it.kode == kode }
    }
}

enum class BelopPeriode(val kode: String, val visningsnavn: String) {
    DAG( "DAG", "Per dag"),
    UKE( "UKE", "Per uke"),
    MND( "MND", "Per måned");

    companion object {
        fun fraKode(kode: String): BelopPeriode? = entries.find { it.kode == kode }
    }
}
