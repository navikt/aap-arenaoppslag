package no.nav.aap.arenaoppslag.kontrakt.apiv1

import java.time.LocalDate

public data class HarHistorikkRequest(
    val personidentifikator: String,
)

public data class SignifikantHistorikkRequest(
    val personidentifikator: String,
    val virkningstidspunkt: LocalDate, // datoen søknaden ble mottatt, feks. per post
)

public data class HarHistorikkResponse(
    val harHistorikk: Boolean,
) {
    public companion object {
        public val ja: HarHistorikkResponse = HarHistorikkResponse(true)
        public val nei: HarHistorikkResponse = HarHistorikkResponse(false)
    }
}

public data class SignifikantHistorikkResponse(
    val harSignifikantHistorikk: Boolean,
    val signifikanteVedtak: List<ArenaVedtak> // signifikante Arena-vedtak, sortert på dato, nyeste først
) {
    public companion object {
        public val ingen: SignifikantHistorikkResponse = SignifikantHistorikkResponse(false, emptyList())
    }
    public fun saker(): List<Int> = signifikanteVedtak.map { it.sakId }
}
