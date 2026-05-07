package no.nav.aap.arenaoppslag.database

import org.intellij.lang.annotations.Language
import java.sql.Connection
import java.time.LocalDate
import javax.sql.DataSource

class PosteringRepository(private val dataSource: DataSource) {

    fun hentNyestePosteringISak(saksId: Int): LocalDate? {
        return dataSource.connection.use { con ->
            selectMaksPosteringsDatoForSak(saksId, con)
        }
    }

    private fun selectMaksPosteringsDatoForSak(saksId: Int, connection: Connection): LocalDate? {
        @Language("OracleSql")
        val query = """
            select 
              max(p.dato_postert) as siste_postering       
            FROM postering p  
              join vedtak v
              on p.vedtak_id = v.vedtak_id
            WHERE v.sak_id = ? 
              AND v.rettighetkode = 'AAP'
            """

        connection.prepareStatement(query).use { preparedStatement ->
            preparedStatement.setInt(1, saksId)
            val resultSet = preparedStatement.executeQuery()
            resultSet.next()
            return resultSet.getDate("siste_postering")?.toLocalDate()
        }
    }
}