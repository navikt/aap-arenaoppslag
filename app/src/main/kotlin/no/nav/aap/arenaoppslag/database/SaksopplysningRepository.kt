package no.nav.aap.arenaoppslag.database

import no.nav.aap.arenaoppslag.modeller.ArenaSaksopplysning
import no.nav.aap.arenaoppslag.modeller.ArenaSaksopplysningAttributt
import org.intellij.lang.annotations.Language
import java.sql.ResultSet
import javax.sql.DataSource

class SaksopplysningRepository(private val dataSource: DataSource) {

    fun hentForVedtakId(vedtakId: Int): List<ArenaSaksopplysning> {
        return dataSource.connection.use { con ->
            con.createParameterizedQuery(selectSaksopplysningerForVedtakId).use { ps ->
                ps.setInt(1, vedtakId)
                ps.executeQuery()
                    .map { row -> mapperForRad(row) }
                    .groupBy { it.saksopplysningId }
                    .map { (_, rader) ->
                        val foerste = rader.first()
                        ArenaSaksopplysning(
                            saksopplysningId = foerste.saksopplysningId,
                            saksopplysningkode = foerste.saksopplysningkode,
                            saksopplysningnavn = foerste.saksopplysningnavn,
                            skjermbildetekst = foerste.saksopplysningSkjermbildetekst,
                            statusRepeterbar = foerste.statusRepeterbar,
                            verdi = foerste.saksopplysningVerdi,
                            attributter = rader.map { rad ->
                                ArenaSaksopplysningAttributt(
                                    attributtkode = rad.attributtkode,
                                    skjermbildetekst = rad.attributtSkjermbildetekst,
                                    formatnavn = rad.formatnavn,
                                    posisjon = rad.posisjon,
                                    verdi = rad.attributtVerdi,
                                    statusSjekketAv = rad.statusSjekketAv,
                                )
                            },
                        )
                    }
            }
        }
    }

    companion object {
        @Language("OracleSql")
        private val selectSaksopplysningerForVedtakId = """
            SELECT s.saksopplysning_id,
                   s.saksopplysningkode,
                   st.saksopplysningnavn,
                   st.skjermbildetekst  AS saksopplysning_skjermbildetekst,
                   st.status_repeterbar,
                   s.verdi              AS saksopplysning_verdi,
                   at.posisjon,
                   at.attributtkode,
                   at.skjermbildetekst  AS attributt_skjermbildetekst,
                   at.formatnavn,
                   a.verdi              AS attributt_verdi,
                   a.status_sjekket_av
              FROM lov_vedtak_saksopplysning lov
              JOIN saksopplysning s  ON s.saksopplysning_id  = lov.saksopplysning_id
              JOIN saksopplysningtype st ON st.saksopplysningkode = s.saksopplysningkode
              JOIN attributtype at  ON at.saksopplysningkode = s.saksopplysningkode
              JOIN attributt a      ON a.saksopplysning_id_eier = s.saksopplysning_id
                                   AND a.attributtype_id        = at.attributtype_id
             WHERE lov.vedtak_id = ?
             ORDER BY s.saksopplysningkode, at.posisjon
        """.trimIndent()

        private data class SaksopplysningRad(
            val saksopplysningId: Long,
            val saksopplysningkode: String,
            val saksopplysningnavn: String,
            val saksopplysningSkjermbildetekst: String?,
            val statusRepeterbar: String,
            val saksopplysningVerdi: String?,
            val posisjon: Int,
            val attributtkode: String,
            val attributtSkjermbildetekst: String?,
            val formatnavn: String?,
            val attributtVerdi: String?,
            val statusSjekketAv: String?,
        )

        private fun mapperForRad(row: ResultSet) = SaksopplysningRad(
            saksopplysningId = row.getLong("saksopplysning_id"),
            saksopplysningkode = row.getString("saksopplysningkode"),
            saksopplysningnavn = row.getString("saksopplysningnavn"),
            saksopplysningSkjermbildetekst = row.getString("saksopplysning_skjermbildetekst"),
            statusRepeterbar = row.getString("status_repeterbar"),
            saksopplysningVerdi = row.getString("saksopplysning_verdi"),
            posisjon = row.getInt("posisjon"),
            attributtkode = row.getString("attributtkode"),
            attributtSkjermbildetekst = row.getString("attributt_skjermbildetekst"),
            formatnavn = row.getString("formatnavn"),
            attributtVerdi = row.getString("attributt_verdi"),
            statusSjekketAv = row.getString("status_sjekket_av"),
        )
    }
}

