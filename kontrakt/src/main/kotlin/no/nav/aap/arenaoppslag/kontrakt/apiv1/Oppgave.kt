package no.nav.aap.arenaoppslag.kontrakt.apiv1

import java.time.LocalDate

// Øvrige felter er nullbare fordi v_oppgave i Arena ikke garanterer utfylte verdier
public data class Oppgave(
    val oppgaveId: Long,
    val beskrivelse: String?,
    val sakskontekst: String?,
    val visningsnavn: String?,
    val fristDato: LocalDate?,
    val arbeidsbenk: String?,
    val oppgaveEnhet: String?,
    val navEnhet: String?,
    val notat: String?,
)

