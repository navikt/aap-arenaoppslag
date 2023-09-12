package arenaoppslag.modell

import java.time.LocalDate

data class Person(
    val personId: Int,
    val fodselsnr: String,
    val statusSamtykke: String,
    val datoSamtykke: LocalDate,
    val vernepliktkode: String,
    val formidlingsgruppekode: String,
    val vikargruppekode: String,
    val klassifiseringsgruppekode: String,
    val rettighetsgruppekode: String,
    val regDato: LocalDate,
    val regUser: String,
    val modDato: LocalDate,
    val modUser: String,
)
