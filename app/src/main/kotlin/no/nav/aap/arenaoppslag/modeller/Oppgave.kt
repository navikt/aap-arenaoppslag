package no.nav.aap.arenaoppslag.modeller

import java.time.LocalDate

// Alle felter er nullbare fordi v_oppgave i Arena ikke garanterer utfylte verdier
data class ArenaOppgave(
    val beskrivelse: String?,
    val sakskontekst: String?,
    val visningsnavn: String?,
    val fristDato: LocalDate?,
    val arbeidsbenk: String?,
    val oppgaveEnhet: String?,
    val navEnhet: String?,
    val notat: String?,
)

