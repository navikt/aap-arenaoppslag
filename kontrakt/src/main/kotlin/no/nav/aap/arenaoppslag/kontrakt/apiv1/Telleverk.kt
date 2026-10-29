package no.nav.aap.arenaoppslag.kontrakt.apiv1

import java.time.LocalDate


public data class TelleverkForPerson(
    val ordineerAAPKvote: Int,
    val utvidetAAPKvote: Int?,
)

public data class TelleverkResponse(
    val telleverk: TelleverkForPerson?,
    val maksdato: SakMedSisteVedtakOgMaksdato?,
    val sisteUtbetalingDato: LocalDate?,
)