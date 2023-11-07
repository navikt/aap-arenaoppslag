package arenaoppslag.dsop

import java.time.LocalDate

data class Periode(
    val fraDato: LocalDate,
    val tilDato: LocalDate
) {
    fun erDatoIPeriode(dato: LocalDate) = dato in fraDato..tilDato

}