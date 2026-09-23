package no.nav.aap.arenaoppslag.modeller.migrering

import no.nav.aap.arenaoppslag.kontrakt.migrering.GjenstaaendeKvote
import no.nav.aap.arenaoppslag.kontrakt.migrering.KravResponse
import java.time.LocalDate

data class Krav(
    val lopenr: Int,
    val aar: Int,
    val soknadsdato: LocalDate?,
    val migreringsdato: LocalDate?,
    val gjenstaaendeOrdinaerKvote: Int?,
) {
    fun tilKontrakt() = KravResponse(
        lopenr = lopenr,
        aar = aar,
        soknadsdato = soknadsdato,
        migreringsdato = migreringsdato,
        gjenstaaendeKvote = GjenstaaendeKvote(ordinaer = gjenstaaendeOrdinaerKvote),
    )
}
