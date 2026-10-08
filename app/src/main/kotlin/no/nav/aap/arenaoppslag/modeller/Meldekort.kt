package no.nav.aap.arenaoppslag.modeller

import java.time.LocalDate

// Domeneobjekt: én posteringslinje for en sak — tilsvarer én rad i tilkjent-ytelse-tabellen.
data class MeldekortPostering(
    // POSTERING_ID + POSTERINGTYPEKODE er primærnøkkelen i POSTERING. ID-en alene er ikke garantert unik.
    val posteringId: Long? = null,
    val posteringTypeKode: String? = null,
    val vedtakId: Int,
    val personId: Int,
    // null betyr utbetaling uten tilknyttet meldekort (f.eks. spesialutbetaling)
    val meldekortId: Long?,
    val periode: Periode,
    val belop: Int,
    // POSTERING.ANTALL — antall dager posteringen dekker. Kan mangle på eldre/manuelle posteringer.
    val antall: Double? = null,
    // Dagsats med barnetillegg (vedtakfakta DAGSMBT) — null hvis ikke registrert på vedtaket
    val dagsatsMedBarnetillegg: Int?,
    // Dagsats uten barnetillegg (vedtakfakta DAGS) — null hvis ikke registrert på vedtaket
    val dagsats: Int?,
    // Dagsats uten barnetillegg FØR samordning (vedtakfakta DAGSFSAM) — brukes til å beregne samordningsprosent
    val dagsatsForSamordning: Int?,
    // Graderingsprosent for reduksjon pga. institusjonsopphold (vedtakfakta INSGRAD) — null hvis ikke registrert
    val insGrad: Int?,
    // Utledet av POSTERING.TABELLNAVNALIAS_KILDE. UKJENT når aliaset mangler eller ikke er kjent for oss.
    val kilde: PosteringKilde = PosteringKilde.UKJENT,
    // Rå aliasverdi, beholdt for å kunne tagge metrikken når vi møter ukjente kilder
    val kildeAlias: String? = null,
    // POSTERING.OBJEKT_ID_KILDE — peker på forekomsten i kildetabellen. Brukes internt, eksponeres ikke.
    val kildeObjektId: Long? = null,
    // Kun satt når kilden er SPESUTB og raden finnes i SPESIALUTBETALING
    val spesialutbetaling: Spesialutbetaling? = null,
)

// Domeneobjekt: én rad fra SPESIALUTBETALING. Alle kolonner kan mangle i Arena, så alt er nullable.
data class Spesialutbetaling(
    val begrunnelse: String?,
    // DECIMAL(12,2) i Arena — kan inneholde øre, i motsetning til POSTERING.BELOP
    val belop: Double?,
    val belopKode: String?,
    val datoUtbetaling: LocalDate?,
    val periode: Periode,
    val vedtakStatusKode: String?,
    val posteringTypeKode: String?,
    val statusBilag: Boolean?,
    val statusAnvistBilag: Boolean?,
    val kategori: String?,
    // Ventebetingelse når utbetalingen er satt på vent (f.eks. REFKRAVSOS, REFKRAVTP, AVREGNAYT)
    val valgtUtbetalingType: String?,
    val saksbehandler: String?,
    val beslutter: String?,
)

data class MeldekortReduksjon(
    val dagerForSent: Int,
    val fravar: Float,
    val sykedager: Float,
)

// Domeneobjekt: én anmerkning registrert på et meldekort (ANMERKNING joinet med ANMERKNINGTYPE).
data class MeldekortAnmerkning(
    val kode: String,
    // Navn og beskrivelse kommer fra kodetabellen ANMERKNINGTYPE og kan mangle for ukjente koder.
    val navn: String?,
    val beskrivelse: String?,
    // Substitusjonsparameter 1 og 2 som flettes inn i beskrivelsen (&1 og &2)
    val verdi: Int?,
    val verdi2: Int?,
    // Vedtaket meldekortet ble beregnet mot da anmerkningen ble laget. null betyr at anmerkningen
    // gjelder selve meldekortet og ikke en bestemt beregning.
    val vedtakId: Int? = null,
)

// Reduksjonstallene er summen av verdiene på de tre anmerkningkodene som påvirker utbetalingen:
// for sent levert meldekort, annet fravær og sykdom.
fun reduksjonFra(anmerkninger: List<MeldekortAnmerkning>) = MeldekortReduksjon(
    dagerForSent = summerVerdi(anmerkninger, "SENN"),
    fravar = summerVerdi(anmerkninger, "FXNN").toFloat(),
    sykedager = summerVerdi(anmerkninger, "FSNN").toFloat(),
)

private fun summerVerdi(anmerkninger: List<MeldekortAnmerkning>, kode: String): Int =
    anmerkninger.filter { it.kode == kode }.sumOf { it.verdi ?: 0 }

// Et meldekort kan beregnes mot flere vedtak, og hver beregning legger igjen egne anmerkninger.
// Reduksjonen regnes på nytt slik at den bare bygger på anmerkningene som er beholdt.
fun Meldekort.medAnmerkninger(behold: (MeldekortAnmerkning) -> Boolean): Meldekort {
    val beholdte = anmerkninger.filter(behold)
    return copy(anmerkninger = beholdte, reduksjon = reduksjonFra(beholdte))
}

// Domeneobjekt: ett meldekort med tilhørende dager og anmerkninger.
data class Meldekort(
    val meldekortId: Long,
    val personId: Int,
    // Meldekortperiodens datoer (mandag i uke 1 til søndag i uke 2)
    val periode: Periode,
    val ukenrUke1: Int,
    val ukenrUke2: Int,
    val meldedato: LocalDate?,
    val meldeform: String?,
    val fortsattRegistrertArbeidssoker: Boolean?,
    val kommentar: String?,
    val dager: List<MeldekortDag>,
    val reduksjon: MeldekortReduksjon,
    val anmerkninger: List<MeldekortAnmerkning> = emptyList(),
    // BEREGNINGSTATUSKODE sier om meldekortet er ferdig beregnet. Uten den kan ikke frontend skille
    // et meldekort som venter på beregning fra et som er beregnet uten utbetaling.
    val beregningStatusKode: String? = null,
)

data class MeldekortDag(
    val ukenr: Int,
    val dagnr: Int,
    val dato: LocalDate,
    val timerArbeidet: Double,
    val annetFravaer: Boolean,
)

// Samlet resultat fra MeldekortRepository for én sak: tabellrader (posteringer) og meldekortdetaljer.
data class MeldekortForSak(
    val posteringer: List<MeldekortPostering>,
    val meldekort: List<Meldekort>,
)

