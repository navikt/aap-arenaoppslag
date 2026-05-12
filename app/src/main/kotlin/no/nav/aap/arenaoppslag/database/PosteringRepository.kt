package no.nav.aap.arenaoppslag.database

import no.nav.aap.arenaoppslag.modeller.PersonId
import org.intellij.lang.annotations.Language
import java.sql.Connection
import java.time.LocalDate
import javax.sql.DataSource

class PosteringRepository(private val dataSource: DataSource) {

    fun hentSisteAapUtbetalingForPerson(personId: PersonId): LocalDate? {
        return dataSource.connection.use { con ->
            selectMaksPosteringsDatoForPerson(personId, con)
        }
    }

    private fun selectMaksPosteringsDatoForPerson(personId: PersonId, connection: Connection): LocalDate? {
        @Language("OracleSql")
        val query = """
            select 
              max(p.dato_postert) as siste_postering       
            FROM postering p  
              join vedtak v
              on p.vedtak_id = v.vedtak_id
            WHERE v.person_id = ? 
              AND v.rettighetkode = 'AAP'
            """

        connection.prepareStatement(query).use { preparedStatement ->
            preparedStatement.setInt(1, personId.id)
            val resultSet = preparedStatement.executeQuery()
            resultSet.next()
            return resultSet.getDate("siste_postering")?.toLocalDate()
        }
    }
}