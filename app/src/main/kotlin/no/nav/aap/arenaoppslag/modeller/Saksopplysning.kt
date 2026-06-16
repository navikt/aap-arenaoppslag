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
    val attributtkode: Attributtkode,
    val skjermbildetekst: String?,
    val formatnavn: String?,
    val posisjon: Int,
    val verdi: String?,
    val statusSjekketAv: String?,
)

enum class Attributtkode {
    STRFG, INSTA, INFRA, INTIL, FRIKL, REDPR,
    TYPE, GRAD, BELOP, BELPR,
    BEGRUNNELSE, DATO,
    UKJENT;

    companion object {
        fun fraKode(kode: String): Attributtkode = entries.find { it.name == kode } ?: UKJENT
    }
}

data class InstitusjonOpphold(
    val type: InstitusjonOppholdType,
    val fra: LocalDate,
    val til: LocalDate?,
    val friKostOgLosji: Boolean,
    val reduksjonsType: ReduksjonType?,
) {
    companion object {
        const val KODE = "INSOPPH"
        val DATO_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
    }

    enum class Attributt(val kode: Attributtkode) {
        STRAFFEGJENNOMFORING(Attributtkode.STRFG),
        INSTA(Attributtkode.INSTA),
        FRA(Attributtkode.INFRA),
        TIL(Attributtkode.INTIL),
        FRI_KOST_LOSJI(Attributtkode.FRIKL),
        REDUKSJON(Attributtkode.REDPR),
    }
}

data class AnnenYtelse(
    val type: AnnenYtelseType,
    val belopPeriode: BelopPeriode?,
    val grad: String?,
    val belop: String?,
) {
    companion object {
        const val KODE = "AAOKYT"
    }
}

data class SamordningOgInstitusjon(
    val institusjonOpphold: InstitusjonOpphold?,
    val andreYtelser: List<AnnenYtelse>,
)


fun ArenaSaksopplysning.tilInstitusjonOpphold(): InstitusjonOpphold? {
    if (saksopplysningkode != InstitusjonOpphold.KODE) return null
    fun attr(a: InstitusjonOpphold.Attributt) = attributter.find { it.attributtkode == a.kode }?.verdi
    val type = when {
        attr(InstitusjonOpphold.Attributt.STRAFFEGJENNOMFORING) == "J" -> InstitusjonOppholdType.FENGSEL
        attr(InstitusjonOpphold.Attributt.INSTA) == "J" -> InstitusjonOppholdType.HELSEINSTITUSJON
        else -> return null
    }
    val fra = attr(InstitusjonOpphold.Attributt.FRA)?.let { LocalDate.parse(it, InstitusjonOpphold.DATO_FORMAT) } ?: return null
    val til = attr(InstitusjonOpphold.Attributt.TIL)?.let { LocalDate.parse(it, InstitusjonOpphold.DATO_FORMAT) }
    val friKostOgLosji = attr(InstitusjonOpphold.Attributt.FRI_KOST_LOSJI) == "J"
    val reduksjonsType = attr(InstitusjonOpphold.Attributt.REDUKSJON)?.let { ReduksjonType.fraKode(it) }
    return InstitusjonOpphold(type, fra, til, friKostOgLosji, reduksjonsType)
}

fun ArenaSaksopplysning.tilAnnenYtelse(): AnnenYtelse? {
    if (saksopplysningkode != AnnenYtelse.KODE) return null
    fun attr(kode: Attributtkode) = attributter.find { it.attributtkode == kode }?.verdi
    val type = attr(Attributtkode.TYPE)?.let { AnnenYtelseType.fraKode(it) } ?: return null
    val belopPeriode = attr(Attributtkode.BELPR)?.let { BelopPeriode.fraKode(it) }
    val grad = attr(Attributtkode.GRAD)
    val beløp = attr(Attributtkode.BELOP)
    return AnnenYtelse(type, belopPeriode, grad, beløp)
}

enum class InstitusjonOppholdType(val kode: String) {
    FENGSEL("FENGSEL"),
    HELSEINSTITUSJON("HELSEINS");
}

enum class ReduksjonType(val kode: String, val prosent: Int) {
    INGEN("RED00", 0),
    HALV("RED50", 50);

    companion object {
        fun fraKode(kode: String): ReduksjonType? = entries.find { it.kode == kode }
    }
}

enum class AnnenYtelseType(val kode: String) {
    FORELDREPENGER_ADOPSJON("AP"),
    BARNEPENSJON(           "BP"),
    OMSORGSPENGER(          "BS"),
    FORELDREPENGER_FODSEL(  "FP"),
    LONN_FRA_ARBEIDSGIVER(  "LØNN"),
    OPPLARINGSPENGER(       "OP"),
    PLEIEPENGER(            "PB"),
    SVANGERSKAPSPENGER(     "SV"),
    UFORETRYGD(             "UP");

    companion object {
        fun fraKode(kode: String): AnnenYtelseType? = entries.find { it.kode == kode }
    }
}

enum class BelopPeriode(val kode: String) {
    DAG("DAG"),
    UKE("UKE"),
    MND("MND");

    companion object {
        fun fraKode(kode: String): BelopPeriode? = entries.find { it.kode == kode }
    }
}

