package arenaoppslag.modell

import java.time.LocalDate

data class Vedtak(
    val vedtakId: Int,
    val sakId: Int,
    val vedtakstatuskode: String,
    val vedtaktypekode: String,
    val personId: String,
    val utfallkode: String,
    val datoMottatt: LocalDate,
    val regDato: LocalDate,
    val regUser: String,
    val modDato: LocalDate,
    val modUser: String,
)
