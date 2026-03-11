package no.nav.aap.arenaoppslag.database;

import org.intellij.lang.annotations.Language
import javax.sql.DataSource;


data class KvoteVerdi(val kode: String, val verdi: Int)


class TelleverkRepository(private val datasource: DataSource) {

    companion object {
        @Language("OracleSql")
        internal val selectTelleverkPåPerson = """
            SELECT b.verdi, b.beregningsleddkode
            FROM BEREGNINGSLEDD b
            JOIN PERSON p ON p.person_id = b.person_id
            WHERE b.tabellnavnalias_kilde = 'KVOTBR'
              AND p.fodselsnr = ?
              AND b.beregningsleddkode IN ('AAP', 'MAAPU')
        """.trimIndent()
    }

    fun hentTelleverkPåPerson(fodselsnr: String): Set<KvoteVerdi> {
        return datasource.connection.use { con ->
            con.prepareStatement(selectTelleverkPåPerson).use { preparedStatement ->
                preparedStatement.setString(1, fodselsnr)
                val resultSet = preparedStatement.executeQuery()
                resultSet.map { row ->
                    KvoteVerdi(
                        kode = row.getString("beregningsleddkode"),
                        verdi = row.getInt("verdi")
                    )
                }.toSet()
            }
        }
    }
}

