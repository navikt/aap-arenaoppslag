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

    fun hentPerioderForMeldekort(meldekortIder: Collection<Long>): Map<Long, Periode> {
        if (meldekortIder.isEmpty()) return emptyMap()
        return dataSource.connection.use { con ->
            // Oracle har en hard grense på 1000 elementer i IN-lister.
            meldekortIder.distinct().chunked(999).flatMap { chunk ->
                con.createParameterizedQuery(perioderForMeldekortSql(chunk)).use { preparedStatement ->
                    preparedStatement.executeQuery().map { row ->
                        row.getLong("meldekort_id") to Periode(
                            fraOgMedDato = row.getDate("dato_fra").toLocalDate(),
                            tilOgMedDato = row.getDate("dato_til").toLocalDate(),
                        )
                    }
                }
            }.toMap()
        }
    }

    companion object {
        // Oracle støtter ikke listeparametere i PreparedStatement, så meldekort-IDer interpoleres direkte.
        // IDene er Long-verdier fra databasen, så det er ingen risiko for SQL-injeksjon.
        private fun perioderForMeldekortSql(meldekortIder: List<Long>): String = """
            SELECT m.meldekort_id, mkp.dato_fra, mkp.dato_til
              FROM meldekort m
              JOIN meldekortperiode mkp ON mkp.aar = m.aar AND mkp.periodekode = m.periodekode
             WHERE m.meldekort_id IN (${meldekortIder.joinToString(",")})
        """.trimIndent()

        @Language("OracleSql")
        private val selectGjeldendePeriode = """
            SELECT dato_fra, dato_til
              FROM meldekortperiode
             WHERE dato_fra <= ? AND dato_til >= ?
        """.trimIndent()
    }
}
