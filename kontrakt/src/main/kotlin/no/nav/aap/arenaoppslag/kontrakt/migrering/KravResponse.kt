package no.nav.aap.arenaoppslag.kontrakt.migrering

import java.time.LocalDate

public data class KravResponse(
    val lopenr: Int,
    val aar: Int,
    val soknadsdato: LocalDate?,
    val migreringsdato: LocalDate?,
    val gjenstaaendeKvote: GjenstaaendeKvote,
)

public data class GjenstaaendeKvote(
    val ordinaer: Int?,
)
