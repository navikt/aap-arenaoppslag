package no.nav.aap.arenaoppslag.kontrakt.intern

import no.nav.aap.arenaoppslag.kontrakt.apiv1.Oppgave
import no.nav.aap.arenaoppslag.kontrakt.apiv1.VedtakMedMaksdato
import no.nav.aap.arenaoppslag.kontrakt.modeller.Periode
import java.time.LocalDate

@Deprecated("Bruk nytt endepunkt person/historikk", level = DeprecationLevel.WARNING)
public data class PersonEksistererIAAPArena(
    val eksisterer: Boolean
)

@Deprecated("Bruk nytt endepunkt person/historikk/signifikant", level = DeprecationLevel.ERROR)
public data class SignifikanteSakerResponse(
    val harSignifikantHistorikk: Boolean,
    val signifikanteSaker: List<String> // signifikante Arena-saker, sortert på dato, nyeste først
)

public data class Person(val personIdentifikator: String, val fornavn: String, val etternavn: String)

public data class PerioderResponse(
    val perioder: List<Periode>
)

public data class PerioderMed11_17Response(
    val perioder: List<PeriodeMed11_17>
)

public data class PeriodeMed11_17(
    val periode: Periode,
    val aktivitetsfaseKode: String,
    val aktivitetsfaseNavn: String
)

public data class SakStatus(
    val sakId: String,
    val statusKode: Status,
    val periode: Periode,
    val kilde: Kilde = Kilde.ARENA
)


public data class ManuellFordelingsgrunnlagResponse(
    val saksnummer: String?,
    val erAktiv: Boolean,
    val under52Uker: Boolean?,
    val gjenståendeOrdinæreDager: Int?,
    val gjenståendeUnntaksDager: Int?,
    val sisteVedtak: VedtakMedMaksdato?,
    val sisteUtbetaling: LocalDate?,
    val oppgaver: List<Oppgave>,
)

public enum class Kilde {
    ARENA,
}

public enum class Status {
    // Arena:
    AVSLU,
    FORDE,
    GODKJ,
    INNST,
    IVERK,
    KONT,
    MOTAT,
    OPPRE,
    REGIS,

    // Begge:
    UKJENT;

    public companion object {
        public fun fraStrengverdi(verdi: String?): Status {
            return (Status.entries.find { it.name == verdi }
                ?: UKJENT)
        }
    }

}