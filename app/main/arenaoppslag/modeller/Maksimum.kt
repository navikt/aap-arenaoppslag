package arenaoppslag.modeller

import arenaoppslag.perioder.Periode


data class Maksimum1 (
    val vedtak: List<Vedtak>
)

data class Maksimum2(
    val vedtak: List<Vedtak>,
    val utbetalinger: List<Utbetaling>,
)

data class Vedtak(
    val utbetaling: List<Utbetaling>,
    val dagsats: Int,
    val status: String, //Hypotese, vedtaksstatuskode
    val saksnummer: String, //hypotese sak_id
    val vedtaksdato: String, //reg_dato
    val periode: Periode,
    val rettighetType: String, ////aktivitetsfase //Aktfasekode
)

data class Utbetaling(
    val utbetalingsgrad: Utbetalingsgrad,
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



/*
{
  Maksimum1: {
    vedtak: [
        {
            utbetaling: [
                {
                    utbetalingsgrad: {
                        kode: "string",
                        termnavn: "string"
                    },
                    periode: {
                        fraDato: "string",
                        tilDato: "string"
                    },
                    belop: 1,
                    dagsats: 1,
                    barnetilegg: 1
                }
            ],
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