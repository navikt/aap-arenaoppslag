package no.nav.aap.arenaoppslag.database

import no.nav.aap.arenaoppslag.modeller.Periode
import org.intellij.lang.annotations.Language
import java.sql.Date
import java.time.LocalDate
import javax.sql.DataSource

class MeldekortperiodeRepository(private val dataSource: DataSource) {
    fun hentGjeldendePeriode(idag: LocalDate): Periode? = dataSource.connection.use { con ->
        con.createParameterizedQuery(selectGjeldendePeriode).use { preparedStatement ->
            preparedStatement.setDate(1, Date.valueOf(idag))
            preparedStatement.setDate(2, Date.valueOf(idag))
            val resultSet = preparedStatement.executeQuery()
            if (resultSet.next()) {
                Periode(
                    fraOgMedDato = resultSet.getDate("dato_fra").toLocalDate(),
                    tilOgMedDato = resultSet.getDate("dato_til").toLocalDate(),
                )
            } else {
                null
            }
        }
    }

    companion object {
        @Language("OracleSql")
        private val selectGjeldendePeriode = """
            SELECT dato_fra, dato_til
              FROM meldekortperiode
             WHERE dato_fra <= ? AND dato_til >= ?
        """.trimIndent()
    }
}
