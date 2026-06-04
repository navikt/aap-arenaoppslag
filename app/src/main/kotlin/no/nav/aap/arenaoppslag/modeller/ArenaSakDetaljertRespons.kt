package no.nav.aap.arenaoppslag.modeller

import java.time.LocalDateTime

@Suppress("MatchingDeclarationName")
data class ArenaSakDetaljertRespons(
    val sakId: String,
    val opprettetAar: Int,
    val lopenr: Int,
    val person: ArenaSakPerson,
    val statuskode: String,
    val statusnavn: String,
    val registrertDato: LocalDateTime,
    val avsluttetDato: LocalDateTime?,
    val vedtak: List<ArenaVedtakMedDetaljer>,
    val telleverkForPerson: TelleverkForPerson?,
    val kvoteHistorikk: Set<KvotebrukHendelse>
)

