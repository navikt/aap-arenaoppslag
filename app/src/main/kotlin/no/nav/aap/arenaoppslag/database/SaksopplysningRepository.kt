package no.nav.aap.arenaoppslag.database

import no.nav.aap.arenaoppslag.modeller.ArenaSaksopplysning
import no.nav.aap.arenaoppslag.modeller.ArenaSaksopplysningAttributt
import no.nav.aap.arenaoppslag.modeller.Attributtkode
import org.intellij.lang.annotations.Language
import java.sql.ResultSet
import javax.sql.DataSource

class SaksopplysningRepository(private val dataSource: DataSource) {

    fun hentForVedtakId(vedtakId: Int): List<ArenaSaksopplysning> =
        hentForVedtakIder(listOf(vedtakId))[vedtakId] ?: emptyList()

    fun hentForVedtakIder(vedtakIder: List<Int>): Map<Int, List<ArenaSaksopplysning>> {
        if (vedtakIder.isEmpty()) return emptyMap()

        return dataSource.connection.use { con ->
            val query = queryMedVedtakIdListe(vedtakIder)
            con.createParameterizedQuery(query).use { ps ->
                ps.executeQuery()
                    .map { row -> mapperForRad(row) }
                    .groupBy { it.vedtakId }
                    .mapValues { (_, rader) -> rader.tilSaksopplysninger() }
            }
        }
    }

    companion object {
        private const val VEDTAK_ID_LISTE_TOKEN = "?:vedtakider"

        private fun queryMedVedtakIdListe(vedtakIder: List<Int>): String {
            val idListe = vedtakIder.joinToString(separator = ",")
            return selectSaksopplysningerForVedtakIder.replace(VEDTAK_ID_LISTE_TOKEN, idListe)
        }


        private fun List<SaksopplysningRad>.tilSaksopplysninger(): List<ArenaSaksopplysning> =
            groupBy { it.saksopplysningId }
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
                                attributtkode = Attributtkode.fraKode(rad.attributtkode),
                                skjermbildetekst = rad.attributtSkjermbildetekst,
                                formatnavn = rad.formatnavn,
                                posisjon = rad.posisjon,
                                verdi = rad.attributtVerdi,
                                statusSjekketAv = rad.statusSjekketAv,
                            )
                        },
                    )
                }

        @Language("OracleSql")
        private val selectSaksopplysningerForVedtakIder = """
            SELECT lov.vedtak_id,
                   s.saksopplysning_id,
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
             WHERE lov.vedtak_id IN ($VEDTAK_ID_LISTE_TOKEN)
             ORDER BY lov.vedtak_id, s.saksopplysningkode, at.posisjon
        """.trimIndent()

        private data class SaksopplysningRad(
            val vedtakId: Int,
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
            vedtakId = row.getInt("vedtak_id"),
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

