package arenaoppslag.modeller

import arenaoppslag.ekstern.Periode
import java.time.LocalDate



data class Maksimum(
    val vedtak: List<Vedtak>,
)

data class Vedtak(
    val utbetaling: List<UtbetalingMedMer>,
    val dagsats: Int,
    val status: String, //Hypotese, vedtaksstatuskode
    val saksnummer: String, //hypotese sak_id
    val vedtaksdato: String, //reg_dato
    val periode: Periode,
    val rettighetsType: String, ////aktivitetsfase //Aktfasekode
    val beregningsgrunnlag: String,
)

data class UtbetalingMedMer(
    val reduksjon: Reduksjon? = null,
    val periode: Periode,
    val belop: Int,
    val dagsats: Int,
    val barnetilegg: Int,
)

//dagsats ligger i vedtaksfakta //barntill
// dagsbeløp med barnetillegg
//alt ligger i vedtakfakta

data class VedtakRequest(
    val personidentifikator: String,
    val fraOgMedDato: LocalDate,
    val tilOgMedDato: LocalDate
)

data class VedtakResponse(
    val perioder: List<VedtakPeriode>
)

data class VedtakPeriode(
    val fraOgMedDato: LocalDate,
    val tilOgMedDato: LocalDate?
)



data class Reduksjon(
    val timerArbeidet: Double,
    val annenReduksjon: AnnenReduksjon
)

data class AnnenReduksjon(
    val sykedager: Float?,
    val sentMeldekort:Boolean?,
    val fraver: Float?
)

/*
{
  Maksimum1: {

    vedtak: [
        {
            dagsats: 1,
            dagsmbt: 1,
            status: "string",
            saksnummer: "string",
            vedtaksdato: "LocalDate",
            periode: {
                fraDato: "LocalDate",
                tilDato: "LocalDate"
            },
            rettighetType: "string", //aktivitetsfase //Aktfasekode
            utbetaling: [
            {
                reduksjon: {
                    timerArbeidet: 5.0,
                    annenReduksjon:{
                        sykedager: 1.0, #dager #sykedager
                        SentMeldekort: 1.0, #timer
                        fravær: 1.0, #timer
                    } # 100% -8 timer
                },
                periode: {
                    fraDato: "string",
                    tilDato: "string" #hent meldekort ut fra periode
                },
                belop: 1,
                dagsats: 1,
                barnetilegg: 1
                }
            ],
        }
    ]
}
}
 */