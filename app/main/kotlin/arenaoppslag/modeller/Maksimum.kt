package arenaoppslag.modeller

import arenaoppslag.ekstern.Periode
import no.nav.aap.arenaoppslag.kontrakt.modeller.Maksimum as KontraktMaksimum


data class Maksimum(
    val vedtak: List<Vedtak>,
) {
    fun tilKontrakt(): KontraktMaksimum {
        return KontraktMaksimum(
            vedtak = vedtak.map { it.tilKontrakt() }
        )
    }
}

data class Vedtak(
    val utbetaling: List<UtbetalingMedMer>,
    val dagsats: Int,
    val status: String, //Hypotese, vedtaksstatuskode
    val saksnummer: String, //hypotese sak_id
    val vedtaksdato: String, //reg_dato
    val vedtaksTypeKode:String,
    val vedtaksTypeNavn: String,
    val periode: Periode,
    val rettighetsType: String, ////aktivitetsfase //Aktfasekode
    val beregningsgrunnlag: Int,
    val barnMedStonad: Int,
) {
    fun tilKontrakt(): no.nav.aap.arenaoppslag.kontrakt.modeller.Vedtak {
        return no.nav.aap.arenaoppslag.kontrakt.modeller.Vedtak(
            utbetaling = utbetaling.map { it.tilKontrakt() },
            dagsats = dagsats,
            status = status,
            saksnummer = saksnummer,
            vedtaksdato = vedtaksdato,
            periode = periode.tilKontrakt(),
            rettighetsType = rettighetsType,
            beregningsgrunnlag = beregningsgrunnlag,
            barnMedStonad = barnMedStonad,
            vedtaksTypeKode = vedtaksTypeKode,
            vedtaksTypeNavn = vedtaksTypeNavn,
        )
    }
}

enum class VedtaksType(
    val kode: String,
    val navn: String
) {
    REAKSJON("A", "Reaksjon"),
    ENDRING("E", "Endring"),
    FORLENGET_VENTETID("F","Forlenget ventetid"),
    GJENOPPTAK("G", "Gjenopptak"),
    KONTROLL("K", "Kontroll"),
    OMGJØR_REAKSJON("M", "Omgjør reaksjon"),
    ANNULER_SANKSJON("N", "Annuller sanksjon"),
    NY_RETTIGHET("O", "Ny rettighet"),
    STANS("S", "Stans"),
    TIDSBEGRENSET_BORTFALL("T", "Tidsbegrenset bortfall")
}


data class UtbetalingMedMer(
    val reduksjon: Reduksjon? = null,
    val periode: Periode,
    val belop: Int,
    val dagsats: Int,
    val barnetilegg: Int,
) {
    fun tilKontrakt(): no.nav.aap.arenaoppslag.kontrakt.modeller.UtbetalingMedMer {
        return no.nav.aap.arenaoppslag.kontrakt.modeller.UtbetalingMedMer(
            reduksjon = reduksjon?.tilKontrakt(),
            periode = periode.tilKontrakt(),
            belop = belop,
            dagsats = dagsats,
            barnetillegg = barnetilegg
        )
    }
}

//dagsats ligger i vedtaksfakta //barntill
// dagsbeløp med barnetillegg
//alt ligger i vedtakfakta


data class Reduksjon(
    val timerArbeidet: Double,
    val annenReduksjon: AnnenReduksjon
) {
    fun tilKontrakt(): no.nav.aap.arenaoppslag.kontrakt.modeller.Reduksjon {
        return no.nav.aap.arenaoppslag.kontrakt.modeller.Reduksjon(
            timerArbeidet = timerArbeidet,
            annenReduksjon = annenReduksjon.tilKontrakt()
        )
    }
}

data class AnnenReduksjon(
    val sykedager: Float?,
    val sentMeldekort: Boolean?,
    val fraver: Float?
) {
    fun tilKontrakt(): no.nav.aap.arenaoppslag.kontrakt.modeller.AnnenReduksjon {
        return no.nav.aap.arenaoppslag.kontrakt.modeller.AnnenReduksjon(
            sykedager = sykedager,
            sentMeldekort = sentMeldekort,
            fraver = fraver
        )
    }
}

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