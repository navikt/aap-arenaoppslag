package arenaoppslag.modell

import java.time.LocalDate

data class RettighetType(
    val rettighetKode: String,
    val rettighetNavn: String,
    val datoGyldigFra: LocalDate,
    val datoGyldigTil: LocalDate,
    val regDato: LocalDate,
    val regUser: String,
    val modDato: LocalDate,
    val modUser: String,
    val sakskode: String,
    val rettighetsklassekode: String,
    val belopkode: String,
)
