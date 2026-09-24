package no.nav.aap.arenaoppslag.modeller.migrering

import no.nav.aap.arenaoppslag.kontrakt.migrering.ArenaSykdomsvurderingResponse
import no.nav.aap.arenaoppslag.kontrakt.migrering.ArenaVilkar
import no.nav.aap.arenaoppslag.modeller.ArenaVilkårsvurdering

data class Sykdomsvurdering(
    val vedtakId: Int,
    val begrunnelse: String?,
    val vilkar: List<ArenaVilkårsvurdering>,
) {
    fun tilKontrakt() = ArenaSykdomsvurderingResponse(
        vedtakId = vedtakId,
        begrunnelse = begrunnelse,
        vilkar = vilkar.map { it.tilKontrakt() },
        diagnoser = emptyList() // TODO
    )
}

private fun ArenaVilkårsvurdering.tilKontrakt() = ArenaVilkar(
        id = vilkårsvurderingId,
        kode = vilkårkode,
        status = statuskode,
        begrunnelse = begrunnelse,
    )