package arenaoppslag.modeller

import arenaoppslag.intern.Periode
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
    val vedtaksId: String,
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
            vedtaksId = vedtaksId,
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
    val barnetillegg: Int,
) {
    fun tilKontrakt(): no.nav.aap.arenaoppslag.kontrakt.modeller.UtbetalingMedMer {
        return no.nav.aap.arenaoppslag.kontrakt.modeller.UtbetalingMedMer(
            reduksjon = reduksjon?.tilKontrakt(),
            periode = periode.tilKontrakt(),
            belop = belop,
            dagsats = dagsats,
            barnetillegg = barnetillegg
        )
    }
}

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
