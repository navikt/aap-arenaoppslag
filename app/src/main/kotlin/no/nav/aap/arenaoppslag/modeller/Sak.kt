package no.nav.aap.arenaoppslag.modeller

import no.nav.aap.arenaoppslag.kontrakt.apiv1.ArenaSakOppsummeringKontrakt
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
    val unntaksvilkaarGjelderFra: LocalDate?,
    val sakRegistrert: LocalDate,
    val sakAvsluttet: LocalDate?,
    val sakStatus: String,
) {
    fun tilKontrakt() =
        no.nav.aap.arenaoppslag.kontrakt.apiv1.SakMedSisteVedtakOgMaksdato(
            sakId, "${opprettetAar}-${lopenr}",
            sakStatus, sakRegistrert, sakAvsluttet,
            unntaksvilkaarGjelderFra,
            harInnvilget11_12(),
            utredesForUfor(),
            erFerdigAvklart(),
            erLopende(),
            no.nav.aap.arenaoppslag.kontrakt.apiv1.VedtakMedMaksdato(
                vedtakId,
                aktfaseKode,
                vedtaktypeKode,
                til,
                fra,
                maxdatoOrdinaer,
                maxdatoUnntak,
                utledMaxdato(),
            )
        )

    private fun utledMaxdato(): LocalDate? = if (harInnvilget11_12()) {
        maxdatoUnntak ?: maxdatoOrdinaer
    } else {
        maxdatoOrdinaer
    }

    fun erLopende(): Boolean {
        // Stansede vedtak (vedtaktypeKode=S) har udefinert maxdato.
        // Vi filtrer også ut vedtak med sjeldne typer som K (kontroll) for nå.
        return vedtaktypeKode in listOf("O", "E", "G") && sakStatus == "AKTIV"
    }

    fun utredesForUfor() = aktfaseKode == "UVUP"
    fun erFerdigAvklart() = aktfaseKode == "FA"
    fun harInnvilget11_12() = unntaksvilkaarGjelderFra != null
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
        saksopplysninger: List<ArenaSaksopplysning>,
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
        saksopplysninger = saksopplysninger,
    )
}

data class ArenaSakPerson(
    val personId: Int,
    val fodselsnummer: String,
    val fornavn: String,
    val etternavn: String,
)

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
