package no.nav.aap.arenaoppslag.kontrakt.apiv1

import java.time.LocalDate

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

