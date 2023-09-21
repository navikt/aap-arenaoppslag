package arenaoppslag.arenamodell

import java.time.LocalDate

data class Sak(
    val sakId: Int,
    val sakstype: String,
    val regDato: LocalDate,
    val regUser: String,
    val modDato: LocalDate,
    val modUser: String,
    val datoAvsluttet: LocalDate,
    val sakstatuskode: String,
    val arkivnokkel: String,
    val erUtland: String
)
