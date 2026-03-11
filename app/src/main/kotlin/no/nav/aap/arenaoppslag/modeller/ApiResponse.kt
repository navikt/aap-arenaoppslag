package no.nav.aap.arenaoppslag.modeller

import java.time.LocalDateTime

data class ArenaSakDetaljertRespons(
    val sakId: String,
    val opprettetAar: Int,
    val lopenr: Int,
    val fodselsnummer: String,
    val statuskode: String,
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
            fodselsnummer = arenaSakMedVedtak.fodselsnummer,
            statuskode = arenaSakMedVedtak.statuskode,
            registrertDato = arenaSakMedVedtak.registrertDato,
            avsluttetDato = arenaSakMedVedtak.avsluttetDato,
            vedtak = arenaSakMedVedtak.vedtak,
            tellerverk = tellerverkPåPerson
        )
    }
}

