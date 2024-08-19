package arenaoppslag.modeller

import arenaoppslag.perioder.Periode


data class Maksimum1 (
    val vedtak: List<Vedtak>
)

data class Maksimum2(
    val vedtak: List<Vedtak>,
    val utbetalinger: List<UtbetalingMedMer>,
)

data class Vedtak(
    val utbetaling: List<UtbetalingMedMer>,
    val dagsats: Int,
    val status: String, //Hypotese, vedtaksstatuskode
    val saksnummer: String, //hypotese sak_id
    val vedtaksdato: String, //reg_dato
    val periode: Periode,
    val rettighetType: String, ////aktivitetsfase //Aktfasekode
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



data class Utbetalingsgrad(
    val kode: String,
    val termnavn: String //TODO: Denne må renskes sammen med øyvind
)

data class Reduksjon(
    val timerArbeidet: Double,
    val annenReduksjon: AnnenReduksjon
)

data class AnnenReduksjon(
    val sukepenger: Float?,
    val sentMeldekort:Boolean?,
    val fraver: Float?
)

/*
{
  Maksimum1: {
     utbetaling: [
                {
                    reduksjon: {
                        timerArbeidet: 5.0,
                        annenReduksjon:{
                            sykepenger: 1.0, #timer
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
            rettighetType: {
                rettighetKode: "string",
                rettighetNavn: "string",
                datoGyldigFra: "LocalDate",
                datoGyldigTil: "LocalDate",
                regDato: "LocalDate",
                regUser: "string",
                modDato: "LocalDate",
                modUser: "string",
                sakskode: "string",
                rettighetsklassekode: "string",
                belopkode: "string"
            }
        }
    ]
}
}
 */