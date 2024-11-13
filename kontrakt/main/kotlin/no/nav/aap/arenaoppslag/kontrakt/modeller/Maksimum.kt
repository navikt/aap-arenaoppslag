package no.nav.aap.arenaoppslag.kontrakt.modeller

public data class Maksimum(
    val vedtak: List<Vedtak>,
)

public data class Vedtak(
    val utbetaling: List<UtbetalingMedMer>,
    val dagsats: Int,
    val status: String, //Hypotese, vedtaksstatuskode
    val saksnummer: String, //hypotese sak_id
    val vedtaksdato: String, //reg_dato
    val periode: Periode,
    val rettighetsType: String, ////aktivitetsfase //Aktfasekode
    val beregningsgrunnlag: Int,
    val barnMedStonad: Int,
)

public data class UtbetalingMedMer(
    val reduksjon: Reduksjon? = null,
    val periode: Periode,
    val belop: Int,
    val dagsats: Int,
    val barnetillegg: Int,
)


public data class Reduksjon(
    val timerArbeidet: Double,
    val annenReduksjon: AnnenReduksjon
)

public data class AnnenReduksjon(
    val sykedager: Float?,
    val sentMeldekort: Boolean?,
    val fraver: Float?
)