package no.nav.aap.arenaoppslag.modeller

import java.time.LocalDateTime

data class ArenaSakDetaljertRespons(
    val sakId: String,
    val opprettetAar: Int,
    val lopenr: Int,
    val person: ArenaSakPerson,
    val statuskode: String,
    val statusnavn: String,
    val registrertDato: LocalDateTime,
    val avsluttetDato: LocalDateTime?,
    val vedtak: List<ArenaVedtakMedFakta>,
    val tellerverk: TellerverkPåPerson?
) {
    companion object {
        fun fromDomain(
            arenaSakMedVedtak: ArenaSakMedVedtak,
            tellerverkPåPerson: TellerverkPåPerson?
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
            tellerverk = tellerverkPåPerson
        )
    }
}

