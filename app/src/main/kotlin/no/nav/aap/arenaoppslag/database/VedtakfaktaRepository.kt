package no.nav.aap.arenaoppslag.database

import no.nav.aap.arenaoppslag.modeller.ArenaVedtakfakta
import org.intellij.lang.annotations.Language
import java.sql.ResultSet
import java.time.LocalDate
import javax.sql.DataSource

class VedtakfaktaRepository(private val dataSource: DataSource) {

    fun hentForVedtakIder(vedtakIder: List<Int>): Map<Int, List<ArenaVedtakfakta>> {
        if (vedtakIder.isEmpty()) return emptyMap()

        return dataSource.connection.use { con ->
            val query = queryMedVedtakIdListe(vedtakIder)
            con.createParameterizedQuery(query).use { preparedStatement ->
                preparedStatement.executeQuery()
                    .map { row -> mapperForVedtakfaktaRad(row) }
                    .groupBy { it.vedtakId }
                    .mapValues { (_, rader) -> rader.map { it.tilArenaVedtakfakta() } }
            }
        }
    }

    companion object {
        private const val VEDTAK_ID_LISTE_TOKEN = "?:vedtakider"

        private fun queryMedVedtakIdListe(vedtakIder: List<Int>): String {
            // Oracle støtter ikke listeparametere i PreparedStatement, så vi interpolerer direkte
            val idListe = vedtakIder.joinToString(separator = ",")
            return selectVedtakfaktaForVedtakIder.replace(VEDTAK_ID_LISTE_TOKEN, idListe)
        }

        @Language("OracleSql")
        private val selectVedtakfaktaForVedtakIder = """
            SELECT vf.vedtak_id, vf.vedtakfaktakode, vf.vedtakverdi, vf.reg_dato, vft.skjermbildetekst
              FROM vedtakfakta vf
              LEFT JOIN vedtakfaktatype vft ON vft.vedtakfaktakode = vf.vedtakfaktakode
             WHERE vf.vedtak_id IN ($VEDTAK_ID_LISTE_TOKEN)
        """.trimIndent()

        private data class VedtakfaktaRad(
            val vedtakId: Int,
            val kode: String,
            val navn: String,
            val verdi: String?,
            val registrertDato: LocalDate,
        ) {
            fun tilArenaVedtakfakta() = ArenaVedtakfakta(
                kode = kode,
                navn = navn,
                verdi = verdi,
                registrertDato = registrertDato,
            )
        }

        private fun mapperForVedtakfaktaRad(row: ResultSet) = VedtakfaktaRad(
            vedtakId = row.getInt("vedtak_id"),
            kode = row.getString("vedtakfaktakode"),
            navn = row.getString("skjermbildetekst"),
            verdi = row.getString("vedtakverdi"),
            registrertDato = row.getDate("reg_dato").toLocalDate(),
        )
    }
}
