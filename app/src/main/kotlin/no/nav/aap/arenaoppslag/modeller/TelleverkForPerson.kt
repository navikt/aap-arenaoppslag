package no.nav.aap.arenaoppslag.modeller

import java.time.LocalDate

data class TelleverkForPerson(
    val ordineerAAPKvote: Int,
    val utvidetAAPKvote: Int?,
)

data class TelleverkResponse(
    val telleverk: TelleverkForPerson?,
    val maksdato: LocalDate?,
    val sisteUtbetalingDato: LocalDate?,
)
