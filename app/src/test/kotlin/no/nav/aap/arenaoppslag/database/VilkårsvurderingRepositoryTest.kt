package no.nav.aap.arenaoppslag.database

import no.nav.aap.arenaoppslag.modeller.ArenaVilkårsvurdering
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class VilkårsvurderingRepositoryTest : H2TestBase("flyway/minimumtest") {

    private val repo = VilkårsvurderingRepository(h2)

    @Test
    fun `henter vilkårsvurderinger for kjent vedtakId`() {
        // vedtak_id=1234 har to vilkårsvurderinger fra V1_6__insert_vilkaarsvurdering.sql
        val resultat = repo.hentForVedtakIder(listOf(1234))

        assertThat(resultat).containsKey(1234)
        val vurderinger = resultat[1234]!!
        assertThat(vurderinger).hasSize(2)

        val oppfylt = vurderinger.find { it.vilkårsvurderingId == 1001L }!!
        assertThat(oppfylt.vilkårkode).isEqualTo("OPPHINST")
        assertThat(oppfylt.statuskode).isEqualTo("J")
        assertThat(oppfylt.statusnavn).isEqualTo("Oppfylt")
        assertThat(oppfylt.begrunnelse).isEqualTo("Vilkåret er oppfylt")
        assertThat(oppfylt.vurdertAv).isEqualTo("TEST01")
        assertThat(oppfylt.erObligatorisk).isTrue()

        val ikkeOppfylt = vurderinger.find { it.vilkårsvurderingId == 1002L }!!
        assertThat(ikkeOppfylt.vilkårkode).isEqualTo("PAASTAND")
        assertThat(ikkeOppfylt.statuskode).isEqualTo("N")
        assertThat(ikkeOppfylt.statusnavn).isEqualTo("Ikke oppfylt")
        assertThat(ikkeOppfylt.begrunnelse).isNull()
    }

    @Test
    fun `returnerer tom map for ukjent vedtakId`() {
        val resultat = repo.hentForVedtakIder(listOf(999999999))

        assertThat(resultat).isEmpty()
    }

    @Test
    fun `returnerer tom map for tom liste`() {
        val resultat = repo.hentForVedtakIder(emptyList())

        assertThat(resultat).isEmpty()
    }

    @Test
    fun `henter vilkårsvurderinger for flere vedtakIder i én spørring`() {
        // vedtak_id=1234 har data, vedtak_id=4321 har ingen
        val resultat = repo.hentForVedtakIder(listOf(1234, 4321))

        assertThat(resultat).containsKey(1234)
        assertThat(resultat).doesNotContainKey(4321)
        assertThat(resultat[1234]).hasSize(2)
    }
}
