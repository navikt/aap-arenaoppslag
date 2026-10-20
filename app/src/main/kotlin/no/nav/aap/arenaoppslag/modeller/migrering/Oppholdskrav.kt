package no.nav.aap.arenaoppslag.modeller.migrering

import no.nav.aap.arenaoppslag.kontrakt.migrering.ArenaOppholdskravResponse
import no.nav.aap.arenaoppslag.kontrakt.migrering.ArenaVilkar
import no.nav.aap.arenaoppslag.modeller.ArenaVilkårsvurdering

data class Oppholdskrav(
    val vedtakId: Int,
    val vilkar: ArenaVilkårsvurdering?,
) {
    fun tilKontrakt() = ArenaOppholdskravResponse(
        vedtakId = vedtakId,
        vilkar = vilkar?.let {
            ArenaVilkar(
                id = it.vilkårsvurderingId,
                kode = it.vilkårkode,
                status = it.statuskode,
                begrunnelse = it.begrunnelse,
            )
        },
    )
}
