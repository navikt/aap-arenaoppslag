package no.nav.aap.arenaoppslag.kontrakt.dsop

import java.time.LocalDate

public data class DsopRequest(
    val personId: String,
    val periode: Periode,
    val samtykkePeriode: Periode
)

public data class Periode(
    val fraDato: LocalDate,
    val tilDato: LocalDate
)


