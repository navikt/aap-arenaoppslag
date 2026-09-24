package no.nav.aap.arenaoppslag.kontrakt.migrering

import java.time.LocalDate

public data class ArenaSykdomsvurderingResponse(
    val vedtakId: Int,
    val begrunnelse: String?,
    val vilkar: List<ArenaVilkar>,
    val diagnoser: List<ArenaDiagnose>,
)

public data class ArenaVilkar(
    val id: Long,
    val kode: String,
    val status: String,
    val begrunnelse: String?,
)

public data class ArenaDiagnose(
    val kodeverk: String,
    val kode: String,
    val type: ArenaDiagnoseType,
    val opprettet: LocalDate
)

public enum class ArenaDiagnoseType {
    HOVEDDIAGNOSE,
    BIDIAGNOSE
}