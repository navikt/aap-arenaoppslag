package no.nav.aap.arenaoppslag.modeller

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

