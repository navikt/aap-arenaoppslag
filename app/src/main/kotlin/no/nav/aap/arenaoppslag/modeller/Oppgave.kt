package no.nav.aap.arenaoppslag.modeller

import java.time.LocalDate

// Øvrige felter er nullbare fordi v_oppgave i Arena ikke garanterer utfylte verdier
data class ArenaOppgave(
    val oppgaveId: Long,
    val beskrivelse: String?,
    val sakskontekst: String?,
    val visningsnavn: String?,
    val fristDato: LocalDate?,
    val arbeidsbenk: String?,
    val oppgaveEnhet: String?,
    val navEnhet: String?,
    val notat: String?,
) {
    fun tilKontrakt(): no.nav.aap.arenaoppslag.kontrakt.apiv1.Oppgave =
        no.nav.aap.arenaoppslag.kontrakt.apiv1.Oppgave(
            oppgaveId = oppgaveId,
            beskrivelse = beskrivelse,
            sakskontekst = sakskontekst,
            visningsnavn = visningsnavn,
            fristDato = fristDato,
            arbeidsbenk = arbeidsbenk,
            oppgaveEnhet = oppgaveEnhet,
            navEnhet = navEnhet,
            notat = notat,
        )
}

