package arenaoppslag.intern

import java.time.LocalDate


data class Periode(
    val fraOgMedDato: LocalDate,
    val tilOgMedDato: LocalDate?
) {
    fun tilKontrakt(): no.nav.aap.arenaoppslag.kontrakt.modeller.Periode {
        return no.nav.aap.arenaoppslag.kontrakt.modeller.Periode(
            fraOgMedDato = fraOgMedDato,
            tilOgMedDato = tilOgMedDato
        )
    }
}

data class VedtakFakta(
    var dagsmbt: Int,
    var barntill: Int,
    var dags: Int,
    var barnmston: Int,
    var dagsfsam: Int
)

data class PeriodeMed11_17(
    val periode: Periode,
    val aktivitetsfaseKode: String,
    val aktivitetsfaseNavn: String
) {
    fun tilKontrakt(): no.nav.aap.arenaoppslag.kontrakt.intern.PeriodeMed11_17 {
        return no.nav.aap.arenaoppslag.kontrakt.intern.PeriodeMed11_17(
            periode = periode.tilKontrakt(),
            aktivitetsfaseKode = aktivitetsfaseKode,
            aktivitetsfaseNavn = aktivitetsfaseNavn
        )
    }
}