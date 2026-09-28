package no.nav.aap.arenaoppslag.modeller

import java.time.LocalDate

data class KvoteVerdi(val kode: String, val verdi: Int)

data class KvotebrukHendelse(
    val id: Int,
    val kvoteTypeKode: String,
    val endringsGrunnlag: String,
    // Peker på objektet som utløste bevegelsen. Når endringsGrunnlag er "MKORT" er dette meldekort_id.
    val objektIdGrunnlag: Long,
    val antallBevegelse: Int,
    val posteringTypeKode: String,
    val datoHendelse: LocalDate,
    val resterende: Int,
    val modUser: String?,
    val begrunnelse: String?,
)
