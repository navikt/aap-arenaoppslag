package no.nav.aap.arenaoppslag.modeller

import java.time.LocalDate


data class TilkjentYtelseResponse(
    val sakId: Int,
    val rader: List<TilkjentYtelseRad>,
)

// Kilden til en postering leses av POSTERING.TABELLNAVNALIAS_KILDE, som peker på tabellen
// utbetalingen stammer fra. MELDEKORT_ID sier bare om posteringen er knyttet til et meldekort,
// og kan være NULL også for ordinære posteringer — derfor kan den ikke brukes til å utlede kilde.
enum class PosteringKilde(val kode: String) {
    MELDEKORT("MKORT"),
    SPESIALUTBETALING("SPESUTB"),
    BETALINGSPLAN("BETPLAN"),
    UKJENT("");

    companion object {
        fun fraKode(kode: String?): PosteringKilde =
            entries.firstOrNull { it.kode == kode } ?: UKJENT
    }
}

data class TilkjentYtelseRad(
    val fraOgMedDato: LocalDate?,
    val tilOgMedDato: LocalDate?,
    val uke: String?,
    val kilde: PosteringKilde,
    val dagsatsMedBarnetillegg: Int?,
    val dagsats: Int?,
    val beregnetBrutto: Int,
    val timerArbeidet: Double?,
    val reduksjon: ReduksjonRespons?,
    val meldekort: MeldekortRespons?,
    val gjenstaaendeOrdinaerDager: Int?,
    val gjenstaaendeUnntakDager: Int?,
)

data class ReduksjonRespons(
    val levertForSentDager: Int,
    val timerArbeidetProsent: Int,
    val samordningsProsent: Int,
    val totalReduksjonProsent: Int,
    val fravar: Float,
    val sykedager: Float,
    val institusjonsProsent: Int?,
)

data class MeldekortRespons(
    val meldekortId: Long,
    val meldedato: LocalDate?,
    val meldeform: String?,
    val fortsattRegistrertArbeidssoker: Boolean?,
    val kommentar: String?,
    val uker: List<MeldekortUkeRespons>,
    val anmerkninger: List<AnmerkningRespons>,
)

data class AnmerkningRespons(
    val kode: String,
    val navn: String?,
    // Rå tekst fra ANMERKNINGTYPE, der &1 og &2 er substitusjonsparametere
    val beskrivelse: String?,
    // Beskrivelsen med &1/&2 erstattet av verdi/verdi2, klar til visning
    val beskrivelseFlettet: String?,
    val verdi: Int?,
    val verdi2: Int?,
)

data class MeldekortUkeRespons(
    val ukenr: Int,
    val dager: List<MeldekortDagRespons>,
)

data class MeldekortDagRespons(
    val dato: LocalDate,
    val timerArbeidet: Double,
    val annetFravaer: Boolean,
)

fun Meldekort.tilRespons(): MeldekortRespons = MeldekortRespons(
    meldekortId = meldekortId,
    meldedato = meldedato,
    meldeform = meldeform,
    fortsattRegistrertArbeidssoker = fortsattRegistrertArbeidssoker,
    kommentar = kommentar,
    uker = dager.groupBy { it.ukenr }
        .toSortedMap()
        .map { (ukenr, dagerForUke) ->
            MeldekortUkeRespons(
                ukenr = ukenr,
                dager = dagerForUke.sortedBy { it.dagnr }.map { it.tilRespons() },
            )
        },
    anmerkninger = anmerkninger.map { it.tilRespons() },
)

fun MeldekortAnmerkning.tilRespons(): AnmerkningRespons = AnmerkningRespons(
    kode = kode,
    navn = navn,
    beskrivelse = beskrivelse,
    beskrivelseFlettet = beskrivelse?.flett(verdi, verdi2),
    verdi = verdi,
    verdi2 = verdi2,
)

// Arena lagrer beskrivelsen med substitusjonsparameterne &1 og &2, som må flettes med
// verdiene fra selve anmerkningen for å gi en lesbar tekst.
private fun String.flett(verdi: Int?, verdi2: Int?): String =
    replace("&1", verdi?.toString() ?: "").replace("&2", verdi2?.toString() ?: "")

fun MeldekortDag.tilRespons(): MeldekortDagRespons = MeldekortDagRespons(
    dato = dato,
    timerArbeidet = timerArbeidet,
    annetFravaer = annetFravaer,
)


