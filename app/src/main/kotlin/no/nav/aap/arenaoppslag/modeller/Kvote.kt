package no.nav.aap.arenaoppslag.modeller

import java.time.LocalDate

data class KvoteVerdi(val kode: String, val verdi: Int)

data class KvotebrukHendelse(
    val id: Int,
    val kvoteTypeKode: String,
    val endringsGrunnlag: String,
    val antallBevegelse: Int,
    val posteringTypeKode: String,
    val datoHendelse: LocalDate,
    val resterende: Int,
    val modUser: String?,
    val begrunnelse: String?,
)
