package no.nav.aap.arenaoppslag.database;

import org.intellij.lang.annotations.Language
import javax.sql.DataSource;


data class KvoteVerdi(val kode: String, val verdi: Int)


class TelleverkRepository(private val datasource: DataSource) {

    companion object {
        @Language("OracleSql")
        internal val selectTelleverkPåPerson = """
        SELECT verdi, beregningsleddkode
FROM BEREGNINGSLEDD
WHERE tabellnavnalias_kilde = 'KVOTBR'
  AND person_id = ?
  AND beregningsleddkode IN ('AAP', 'MAAPU');
    """.trimIndent()

    }


    fun hentTelleverkPåPerson(arenaPersonId: Int): Set<KvoteVerdi> {
        return datasource.connection.use { con ->
            con.prepareStatement(selectTelleverkPåPerson).use { preparedStatement ->
                preparedStatement.setInt(1, arenaPersonId)
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

