package no.nav.aap.arenaoppslag.modeller

import no.nav.aap.arenaoppslag.kontrakt.apiv1.ArenaSakMedVedtakResponse
import no.nav.aap.arenaoppslag.kontrakt.apiv1.ArenaSakOppsummeringKontrakt
import no.nav.aap.arenaoppslag.kontrakt.apiv1.ArenaSakPerson as ArenaSakPersonKontrakt
import java.time.LocalDate
import java.time.LocalDateTime


data class ArenaSakOppsummering(
    val sakId: String,
    val lopenummer: Int,
    val aar: Int,
    val antallVedtak: Int,
    val sakstype: String?,
    val statuskode: String,
    val statusnavn: String,
    val regDato: LocalDate,
    val avsluttetDato: LocalDate?,
) {
    fun tilKontrakt() = ArenaSakOppsummeringKontrakt(
        sakId = sakId,
        lopenummer = lopenummer,
        aar = aar,
        antallVedtak = antallVedtak,
        sakstype = sakstype,
        regDato = regDato,
        avsluttetDato = avsluttetDato,
        statuskode = statuskode,
        statusnavn = statusnavn,
    )
}

data class ArenaSak(
    val sakId: String,
    val opprettetAar: Int,
    val lopenr: Int,
    val person: ArenaSakPerson,
    val statuskode: String,
    val statusnavn: String,
    val registrertDato: LocalDateTime,
    val avsluttetDato: LocalDateTime?,
)

data class Maksdatolinje(
    val sakId: Int,
    val opprettetAar: Int,
    val lopenr: Int,
    val vedtakId: Int,
    val aktfaseKode: String,
    val vedtaktypeKode: String,
    val til: LocalDate?,
    val fra: LocalDate?,
    val maxdatoUnntak: LocalDate?,
    val maxdatoOrdinaer: LocalDate?,
    val sakRegistrert: LocalDate,
    val sakAvsluttet: LocalDate?,
    val sakStatus: String,
) {
    fun tilKontrakt() =
        no.nav.aap.arenaoppslag.kontrakt.apiv1.SakMedSisteVedtakOgMaksdato(
            sakId, "${opprettetAar}-${lopenr}",
            sakStatus, sakRegistrert, sakAvsluttet,
            null,
            null,
            false,
            false,
            false,
            false,
            no.nav.aap.arenaoppslag.kontrakt.apiv1.VedtakMedMaksdato(
                vedtakId,
                aktfaseKode,
                vedtaktypeKode,
                fra,
                til,
                maxdatoOrdinaer,
                maxdatoUnntak,
                maxdatoUnntak ?: maxdatoOrdinaer,
            )
        )

}

data class ArenaSakMedVedtak(
    val sakId: String,
    val opprettetAar: Int,
    val lopenr: Int,
    val person: ArenaSakPerson,
    val statuskode: String,
    val statusnavn: String,
    val registrertDato: LocalDateTime,
    val avsluttetDato: LocalDateTime?,
    val vedtak: List<ArenaVedtakMedDetaljer>
) {
    fun tilKontrakt(
        telleverkForPerson: TelleverkForPerson?,
        kvoteHistorikk: Set<KvotebrukHendelse>,
        sisteUtbetalingDato: LocalDate?,
        maksdato: LocalDate?,
    ) = ArenaSakDetaljert(
        sakId = sakId,
        opprettetAar = opprettetAar,
        lopenr = lopenr,
        person = person,
        statuskode = statuskode,
        statusnavn = statusnavn,
        registrertDato = registrertDato,
        avsluttetDato = avsluttetDato,
        vedtak = vedtak,
        telleverkForPerson = telleverkForPerson,
        kvoteHistorikk = kvoteHistorikk,
        maksdato = maksdato,
        sisteUtbetalingDato = sisteUtbetalingDato,
    )

    fun tilKontrakt() = ArenaSakMedVedtakResponse(
        sakId = sakId,
        opprettetAar = opprettetAar,
        lopenr = lopenr,
        person = person.tilKontrakt(),
        statuskode = statuskode,
        statusnavn = statusnavn,
        registrertDato = registrertDato,
        avsluttetDato = avsluttetDato,
        vedtak = vedtak.map { it.tilKontrakt() },
    )
}

data class ArenaSakPerson(
    val personId: Int,
    val fodselsnummer: String,
    val fornavn: String,
    val etternavn: String,
) {
    fun tilKontrakt() = ArenaSakPersonKontrakt(
        personId = personId,
        fodselsnummer = fodselsnummer,
        fornavn = fornavn,
        etternavn = etternavn,
    )
}

fun ArenaSak.toArenaSakMedVedtak(vedtak: List<ArenaVedtakMedDetaljer>) =
    ArenaSakMedVedtak(
        sakId = sakId,
        opprettetAar = opprettetAar,
        lopenr = lopenr,
        person = person,
        registrertDato = registrertDato,
        avsluttetDato = avsluttetDato,
        statuskode = statuskode,
        statusnavn = statusnavn,
        vedtak = vedtak
    )
