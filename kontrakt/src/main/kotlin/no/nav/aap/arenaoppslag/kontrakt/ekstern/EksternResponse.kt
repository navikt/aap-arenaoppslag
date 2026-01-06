package no.nav.aap.arenaoppslag.kontrakt.ekstern

import no.nav.aap.arenaoppslag.kontrakt.modeller.Periode

public data class VedtakResponse(
    val perioder: List<Periode>
)
