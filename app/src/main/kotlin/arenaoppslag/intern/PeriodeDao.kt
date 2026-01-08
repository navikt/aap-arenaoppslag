package arenaoppslag.intern

import arenaoppslag.datasource.map
import arenaoppslag.intern.DbDato.fraDato
import org.intellij.lang.annotations.Language
import java.sql.Connection
import java.sql.Date
import java.time.LocalDate

object PeriodeDao {

    @Language("OracleSql")
    private val selectVedtakMedTidsbegrensningSql = """
        SELECT til_dato, fra_dato
          FROM vedtak 
         WHERE person_id = 
               (SELECT person_id 
                  FROM person 
                 WHERE fodselsnr = ?) 
           AND utfallkode = 'JA' 
           AND rettighetkode = 'AAP'
           AND vedtaktypekode IN ('O', 'E', 'G')
           AND vedtakstatuskode IN ('IVERK', 'AVSLU')
           AND (fra_dato <= til_dato OR til_dato IS NULL)
           AND (til_dato >= ? OR til_dato IS NULL) 
           AND fra_dato <= ?
    """.trimIndent()

    @Language("OracleSql")
    private val selectVedtakMedTidsbegrensningMed11_17Sql = """
        SELECT v.til_dato, v.fra_dato, af.aktfasenavn, v.aktfasekode
          FROM vedtak v
          JOIN aktivitetfase af ON v.aktfasekode = af.aktfasekode
         WHERE person_id = 
               (SELECT person_id 
                  FROM person 
                 WHERE fodselsnr = ?) 
           AND utfallkode = 'JA' 
           AND rettighetkode = 'AAP'
           AND vedtaktypekode IN ('O', 'E', 'G')
           AND vedtakstatuskode IN ('IVERK', 'AVSLU')
           AND (fra_dato <= til_dato OR til_dato IS NULL)
           AND (til_dato >= ? OR til_dato IS NULL) 
           AND fra_dato <= ?
    """.trimIndent()

    fun selectVedtakPerioder(
        fodselsnr: String,
        fraOgMedDato: LocalDate,
        tilOgMedDato: LocalDate,
        connection: Connection
    ): List<Periode> {
        return connection.prepareStatement(selectVedtakMedTidsbegrensningSql)
            .use { preparedStatement ->
                preparedStatement.setString(1, fodselsnr)
                preparedStatement.setDate(2, Date.valueOf(fraOgMedDato))
                preparedStatement.setDate(3, Date.valueOf(tilOgMedDato))

                val resultSet = preparedStatement.executeQuery()

                val perioder = resultSet.map { row ->
                    Periode(
                        fraOgMedDato = row.getDate("fra_dato").toLocalDate(),
                        tilOgMedDato = fraDato(row.getDate("til_dato")),
                    )
                }.toList()

                perioder
            }
    }

    fun selectVedtakMedTidsbegrensningOg11_17(
        personId: String,
        fraOgMedDato: LocalDate,
        tilOgMedDato: LocalDate,
        connection: Connection
    ): List<PeriodeMed11_17> {
        return connection.prepareStatement(selectVedtakMedTidsbegrensningMed11_17Sql)
            .use { preparedStatement ->
                preparedStatement.setString(1, personId)
                preparedStatement.setDate(2, Date.valueOf(fraOgMedDato))
                preparedStatement.setDate(3, Date.valueOf(tilOgMedDato))

                val resultSet = preparedStatement.executeQuery()

                val perioder = resultSet.map { row ->
                    PeriodeMed11_17(
                        Periode(
                            fraOgMedDato = row.getDate("fra_dato").toLocalDate(),
                            tilOgMedDato = fraDato(row.getDate("til_dato")),
                        ),
                        aktivitetsfaseKode = row.getString("aktfasekode"),
                        aktivitetsfaseNavn = row.getString("aktfasenavn"),
                    )
                }
                perioder
            }
    }

}
