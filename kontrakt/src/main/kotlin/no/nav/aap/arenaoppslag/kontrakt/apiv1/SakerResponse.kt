package no.nav.aap.arenaoppslag.kontrakt.apiv1

import java.time.LocalDate

public data class SakerRequest(
    val personidentifikator: String
)

public data class SakerResponse(
    val saker: List<ArenaSak>
)

public data class ArenaSak(
    val sakId: String,
    val lopenummer: Int,
    val aar: Int,
    val antallVedtak: Int,
    val sakstype: String,
    val regDato: LocalDate,
    val avsluttetDato: LocalDate?,
)
