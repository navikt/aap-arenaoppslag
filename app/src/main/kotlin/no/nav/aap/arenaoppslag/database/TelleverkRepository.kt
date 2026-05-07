package no.nav.aap.arenaoppslag.database;

import no.nav.aap.arenaoppslag.modeller.PersonId
import org.intellij.lang.annotations.Language
import javax.sql.DataSource;


data class KvoteVerdi(val kode: String, val verdi: Int)


class TelleverkRepository(private val datasource: DataSource) {

    companion object {
        @Language("OracleSql")
        internal val selectTelleverkPåPerson = """
            SELECT b.verdi, b.beregningsleddkode
            FROM BEREGNINGSLEDD b
            WHERE b.tabellnavnalias_kilde = 'KVOTBR'
              AND b.person_id = ?
              AND b.beregningsleddkode IN ('AAP', 'MAAPU')
        """.trimIndent()
    }

    fun hentTelleverkPåPerson(personId: PersonId): Set<KvoteVerdi> {
        return datasource.connection.use { con ->
            con.prepareStatement(selectTelleverkPåPerson).use { preparedStatement ->
                preparedStatement.setInt(1, personId.id)
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
