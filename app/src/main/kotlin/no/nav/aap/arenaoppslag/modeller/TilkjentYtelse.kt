package no.nav.aap.arenaoppslag.modeller

import java.time.LocalDate


data class TilkjentYtelseResponse(
    val sakId: Int,
    val gjenstaaendeOrdinaerDager: Int?,
    val gjenstaaendeUnntakDager: Int?,
    val rader: List<TilkjentYtelseRad>,
)

data class TilkjentYtelseRad(
    val fraOgMedDato: LocalDate?,
    val tilOgMedDato: LocalDate?,
    // Ukenummer for meldekortperioden, f.eks. "10-11". Null for utbetalinger uten meldekort.
    val uke: String?,
    val kilde: String,
    val dagsatsMedBarnetillegg: Int?,
    val dagsats: Int?,
    val beregnetBrutto: Int,
    val timerArbeidet: Double?,
    val reduksjon: ReduksjonRespons?,
    val meldekort: MeldekortRespons?,
    // Gjenstående dager på ordinær kvote (KVOTEBRUK 'AAP') etter at dette meldekortet ble beregnet.
    // Null for rader uten meldekort, siden kvotetrekk kun skjer per meldekort.
    val gjenstaaendeOrdinaerDager: Int? = null,
    // Gjenstående dager på unntakskvote (KVOTEBRUK 'MAAPU') etter at dette meldekortet ble beregnet.
    val gjenstaaendeUnntakDager: Int? = null,
)

data class ReduksjonRespons(
    // Antall dager forrige meldekort ble levert for sent — disse dagene trekkes fra beregningsgrunnlaget
    val levertForSentDager: Int,
    // Arbeidede timer som prosent av aktiv periode (14 dager minus dager levert for sent, à 7,5 timer)
    val timerArbeidetProsent: Int,
    // Reduksjon i prosent som følge av samordning (DAGSFSAM mot DAGS)
    val samordningsProsent: Int,
    // Total reduksjon: timerArbeidetProsent + samordningsProsent + institusjonsProsent
    val totalReduksjonProsent: Int,
    // Antall dager med annet fravær (anmerkningkode FXNN)
    val fravar: Float,
    // Antall sykedager (anmerkningkode FSNN)
    val sykedager: Float,
    // Graderingsprosent for reduksjon pga. institusjonsopphold (vedtakfakta INSGRAD) — null hvis ikke registrert
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


