package no.nav.aap.arenaoppslag.kontrakt.migrering

import no.nav.aap.arenaoppslag.kontrakt.modeller.Periode
import java.time.LocalDate

public data class KravResponse(
    val lopenr: Int,
    val aar: Int,
    val soknadsdato: LocalDate?,
    val migreringsdato: LocalDate,
    val gjenstaaendeKvote: GjenstaaendeKvote,
    val sisteFerdigBehandledeMeldekortperiode: Periode? = null,
)

public data class GjenstaaendeKvote(
    val ordinaer: Int?,
)
