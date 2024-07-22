package arenaoppslag.modeller

import arenaoppslag.arenamodell.RettighetType
import arenaoppslag.dsop.Periode

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
    val status: String,
    val saksnummer: String,
    val vedtaksdato: String,
    val periode: Periode,
    val rettighetType: RettighetType, //TODO: trenger vi alt dette?
)

data class Utbetaling(
    val utbetalingsgrad: Utbetalingsgrad,
    val periode: Periode,
    val belop: Int,
    val dagsats: Int,
    val barnetilegg: Int,
)

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