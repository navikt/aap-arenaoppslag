package arenaoppslag.ekstern

import java.time.LocalDate

data class Periode(
    val fraOgMedDato: LocalDate,
    val tilOgMedDato: LocalDate?
) {
    fun tilKontrakt(): no.nav.aap.arenaoppslag.kontrakt.modeller.Periode {
        return no.nav.aap.arenaoppslag.kontrakt.modeller.Periode(fraOgMedDato, tilOgMedDato)
    }
}

/**
 * @param barntill Totalt barnetillegg.
 */
data class VedtakFakta(
    var dagsmbt: Int,
    var barntill: Int,
    var dags: Int,
    var dagsfs: Int,
    var barnmston: Int
)

