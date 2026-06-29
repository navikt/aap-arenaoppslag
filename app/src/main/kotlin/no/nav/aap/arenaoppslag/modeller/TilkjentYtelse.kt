package no.nav.aap.arenaoppslag.modeller

import java.time.LocalDate

// Responsobjekter for GET /api/intern/sak/{sakid}/tilkjent-ytelse.
// /api/intern er backend-for-frontend, så formen følger frontend-visningen (se designskisse), ikke REST-konvensjoner.
data class TilkjentYtelseResponse(
    val sakId: Int,
    val gjenstaaendeOrdinaerDager: Int?,
    val gjenstaaendeUnntakDager: Int?,
    val rader: List<TilkjentYtelseRad>,
)

data class TilkjentYtelseRad(
    val fraOgMedDato: LocalDate,
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
)

data class ReduksjonRespons(
    val sykedager: Float,
    val levertForSent: Boolean,
    val fravaer: Float,
)

data class MeldekortRespons(
    val meldekortId: Long,
    val meldedato: LocalDate?,
    val meldeform: String?,
    val fortsattRegistrertArbeidssoker: Boolean?,
    val kommentar: String?,
    val uker: List<MeldekortUkeRespons>,
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
)

fun MeldekortDag.tilRespons(): MeldekortDagRespons = MeldekortDagRespons(
    dato = dato,
    timerArbeidet = timerArbeidet,
    annetFravaer = annetFravaer,
)

fun AnnenReduksjon.tilReduksjonRespons(): ReduksjonRespons = ReduksjonRespons(
    sykedager = sykedager,
    levertForSent = sentMeldekort,
    fravaer = fraver,
)

