package no.nav.aap.arenaoppslag.modeller

import no.nav.aap.arenaoppslag.kontrakt.intern.PeriodeMed11_17
import java.time.DayOfWeek
import java.time.LocalDate


data class Periode(
    val fraOgMedDato: LocalDate?,
    val tilOgMedDato: LocalDate?
) {
    // Åpne perioder har ikke et endelig antall dager, så vi returnerer null i stedet for å gjette
    fun antallHverdager(): Int? {
        val fra = fraOgMedDato ?: return null
        val til = tilOgMedDato ?: return null
        if (til.isBefore(fra)) return 0

        return fra.datesUntil(til.plusDays(1))
            .filter { it.dayOfWeek != DayOfWeek.SATURDAY && it.dayOfWeek != DayOfWeek.SUNDAY }
            .count()
            .toInt()
    }

    fun tilKontrakt(): no.nav.aap.arenaoppslag.kontrakt.modeller.Periode {
        return no.nav.aap.arenaoppslag.kontrakt.modeller.Periode(
            fraOgMedDato = fraOgMedDato,
            tilOgMedDato = tilOgMedDato
        )
    }
}

data class PeriodeMed11_17(
    val periode: Periode,
    val aktivitetsfaseKode: String,
    val aktivitetsfaseNavn: String
) {
    fun tilKontrakt(): PeriodeMed11_17 {
        return PeriodeMed11_17(
            periode = periode.tilKontrakt(),
            aktivitetsfaseKode = aktivitetsfaseKode,
            aktivitetsfaseNavn = aktivitetsfaseNavn
        )
    }
}
