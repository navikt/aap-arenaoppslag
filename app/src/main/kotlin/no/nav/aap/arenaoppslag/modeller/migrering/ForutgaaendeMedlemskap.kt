package no.nav.aap.arenaoppslag.modeller.migrering

import no.nav.aap.arenaoppslag.kontrakt.migrering.ArenaForutgaaendeMedlemskapResponse
import no.nav.aap.arenaoppslag.kontrakt.migrering.ArenaVilkar
import no.nav.aap.arenaoppslag.modeller.ArenaVilkårsvurdering

data class ForutgaaendeMedlemskap(
    val vedtakId: Int,
    val vilkar: ArenaVilkårsvurdering?,
) {
    fun tilKontrakt() = ArenaForutgaaendeMedlemskapResponse(
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
