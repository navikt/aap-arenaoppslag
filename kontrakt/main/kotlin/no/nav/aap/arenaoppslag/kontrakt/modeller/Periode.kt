package no.nav.aap.arenaoppslag.kontrakt.modeller

import java.time.LocalDate

public data class Periode(
    val fraOgMedDato: LocalDate?,
    val tilOgMedDato: LocalDate?
) {
    init {
        if (fraOgMedDato != null && tilOgMedDato != null && tilOgMedDato != fraOgMedDato) {
            require(tilOgMedDato.isAfter(fraOgMedDato)){
                "Til og med dato må være etter fra og med dato { fraOgMedDato=$fraOgMedDato, tilOgMedDato=$tilOgMedDato }"
            }
        }
    }
}