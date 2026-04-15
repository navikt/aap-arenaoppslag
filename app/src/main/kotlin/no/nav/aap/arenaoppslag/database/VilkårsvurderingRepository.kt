package no.nav.aap.arenaoppslag.database

import no.nav.aap.arenaoppslag.modeller.ArenaVilkårsvurdering
import org.intellij.lang.annotations.Language
import java.sql.ResultSet
import javax.sql.DataSource

class VilkårsvurderingRepository(private val dataSource: DataSource) {

    fun hentForVedtakIder(vedtakIder: List<Int>): Map<Int, List<ArenaVilkårsvurdering>> {
        if (vedtakIder.isEmpty()) return emptyMap()

        return dataSource.connection.use { con ->
            val query = queryMedVedtakIdListe(vedtakIder)
            con.createParameterizedQuery(query).use { preparedStatement ->
                preparedStatement.executeQuery()
                    .map { row -> mapperForVilkårsvurderingRad(row) }
                    .groupBy { it.first }
                    .mapValues { (_, rader) -> rader.map { it.second } }
            }
        }
    }

    companion object {
        private const val VEDTAK_ID_LISTE_TOKEN = "?:vedtakider"

        private fun queryMedVedtakIdListe(vedtakIder: List<Int>): String {
            // Oracle støtter ikke listeparametere i PreparedStatement, så vi interpolerer direkte
            val idListe = vedtakIder.joinToString(separator = ",")
            return selectVilkårsvurderingerForVedtakIder.replace(VEDTAK_ID_LISTE_TOKEN, idListe)
        }

        @Language("OracleSql")
        private val selectVilkårsvurderingerForVedtakIder = """
            SELECT vv.vilkaarvurdering_id, vv.vedtak_id, vv.vilkaarkode, vv.begrunnelse, vv.vurdert_av,
                   vt.skjermbildetekst, vt.status_oblig, vt.url_hjelpereferanse, vt.url_lovtekst, vt.url_rundskrivtekst,
                   vs.vilkaarstatuskode, vs.vilkaarstatusnavn
              FROM vilkaarvurdering vv
              LEFT JOIN vilkaartype vt ON vt.vilkaarkode = vv.vilkaarkode
              LEFT JOIN vilkaarstatus vs ON vs.vilkaarstatuskode = vv.vilkaarstatuskode
             WHERE vv.vedtak_id IN ($VEDTAK_ID_LISTE_TOKEN)
        """.trimIndent()

        private fun mapperForVilkårsvurderingRad(row: ResultSet): Pair<Int, ArenaVilkårsvurdering> {
            val vedtakId = row.getInt("vedtak_id")
            val vilkårsvurdering = ArenaVilkårsvurdering(
                vilkårsvurderingId = row.getLong("vilkaarvurdering_id"),
                vilkårkode = row.getString("vilkaarkode"),
                begrunnelse = row.getString("begrunnelse"),
                vurdertAv = row.getString("vurdert_av"),
                vilkårnavn = row.getString("skjermbildetekst"),
                erObligatorisk = row.getString("status_oblig") == "J",
                hjelpetekstUrl = row.getString("url_hjelpereferanse"),
                lovtekstUrl = row.getString("url_lovtekst"),
                rundskrivUrl = row.getString("url_rundskrivtekst"),
                statuskode = row.getString("vilkaarstatuskode"),
                statusnavn = row.getString("vilkaarstatusnavn"),
            )
            return vedtakId to vilkårsvurdering
        }
    }
}
