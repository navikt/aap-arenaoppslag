package no.nav.aap.arenaoppslag.modeller

import no.nav.aap.arenaoppslag.database.KvotebrukHendelse
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
) {
    companion object {
        fun fromDomain(
            arenaSakMedVedtak: ArenaSakMedVedtak,
            telleverkForPerson: TelleverkForPerson?,
            kvoteHistorikk: Set<KvotebrukHendelse>
        ) = ArenaSakDetaljertRespons(
            sakId = arenaSakMedVedtak.sakId,
            opprettetAar = arenaSakMedVedtak.opprettetAar,
            lopenr = arenaSakMedVedtak.lopenr,
            person = arenaSakMedVedtak.person,
            statuskode = arenaSakMedVedtak.statuskode,
            statusnavn = arenaSakMedVedtak.statusnavn,
            registrertDato = arenaSakMedVedtak.registrertDato,
            avsluttetDato = arenaSakMedVedtak.avsluttetDato,
            vedtak = arenaSakMedVedtak.vedtak,
            telleverkForPerson = telleverkForPerson,
            kvoteHistorikk = kvoteHistorikk
        )
    }
}

