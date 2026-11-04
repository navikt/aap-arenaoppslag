package no.nav.aap.arenaoppslag.kontrakt.apiv1

import java.time.LocalDate


public data class TelleverkForPerson(
    val ordineerAAPKvote: Int,
    val utvidetAAPKvote: Int?,
) {
    public fun oppbrukt(): Boolean = ordineerAAPKvote <= 0 && (utvidetAAPKvote ?: 0) <= 0
}

public data class TelleverkResponse(
    val telleverk: TelleverkForPerson?,
    val maksdato: SakMedSisteVedtakOgMaksdato?,
    val sisteUtbetalingDato: LocalDate?,
)