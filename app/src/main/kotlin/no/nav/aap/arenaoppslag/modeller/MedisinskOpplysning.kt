package no.nav.aap.arenaoppslag.modeller

import no.nav.aap.arenaoppslag.kontrakt.migrering.ArenaDiagnose
import java.time.LocalDate

data class MedisinskOpplysning(
    val medisinskOpplysningId: Long,
    val diagnoseklassenavn: String,
    val diagnosekode: String,
    val diagnosetypekode: String,
    val kildeDato: LocalDate,
) {
    fun tilKontrakt() = ArenaDiagnose(
        kodeverk = diagnoseklassenavn,
        kode = diagnosekode,
        type = diagnosetypekode,
        opprettet = kildeDato,
    )
}
