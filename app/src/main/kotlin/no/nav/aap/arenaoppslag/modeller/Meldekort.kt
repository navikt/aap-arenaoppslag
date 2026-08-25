package no.nav.aap.arenaoppslag.modeller

import java.time.LocalDate

// Domeneobjekt: én posteringslinje for en sak — tilsvarer én rad i tilkjent-ytelse-tabellen.
data class MeldekortPostering(
    val vedtakId: Int,
    val personId: Int,
    // null betyr utbetaling uten tilknyttet meldekort (f.eks. spesialutbetaling)
    val meldekortId: Long?,
    val periode: Periode,
    val belop: Int,
    // Dagsats med barnetillegg (vedtakfakta DAGSMBT) — null hvis ikke registrert på vedtaket
    val dagsatsMedBarnetillegg: Int?,
    // Dagsats uten barnetillegg (vedtakfakta DAGS) — null hvis ikke registrert på vedtaket
    val dagsats: Int?,
    // Dagsats uten barnetillegg FØR samordning (vedtakfakta DAGSFSAM) — brukes til å beregne samordningsprosent
    val dagsatsForSamordning: Int?,
    // Graderingsprosent for reduksjon pga. institusjonsopphold (vedtakfakta INSGRAD) — null hvis ikke registrert
    val insGrad: Int?,
)

data class MeldekortReduksjon(
    val dagerForSent: Int,
    val fravar: Float,
    val sykedager: Float,
)

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

