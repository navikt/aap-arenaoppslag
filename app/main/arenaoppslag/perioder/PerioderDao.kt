package arenaoppslag.perioder

import arenaoppslag.datasource.map
import java.sql.Connection
import java.sql.Date
import java.time.LocalDate

object PerioderDao {
    private const val selectVedtakMedTidsbegrensningSql = """
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
    """

    private const val selectVedtakMedTidsbegrensningMed11_17Sql = """
        SELECT v.til_dato, v.fra_dato, af.aktfasenavn 
          FROM vedtak v
          JOIN aktivitetsfase af ON v.aktfasekode = af.aktfasekode
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
    """

    fun selectVedtakMedTidsbegrensning(
        personId: String,
        fraOgMedDato: LocalDate,
        tilOgMedDato: LocalDate,
        connection: Connection
    ): PerioderResponse {
        return connection.prepareStatement(selectVedtakMedTidsbegrensningSql).use { preparedStatement ->
            preparedStatement.setString(1, personId)
            preparedStatement.setDate(2, Date.valueOf(fraOgMedDato))
            preparedStatement.setDate(3, Date.valueOf(tilOgMedDato))

            val resultSet = preparedStatement.executeQuery()

            val perioder = resultSet.map { row ->
                Periode(
                    fraOgMedDato = row.getDate("fra_dato").toLocalDate(),
                    tilOgMedDato = getNullableDate(row.getDate("til_dato")),
                )
            }
            PerioderResponse(perioder)
        }
    }

    fun selectVedtakMedTidsbegrensningOg11_17(
        personId: String,
        fraOgMedDato: LocalDate,
        tilOgMedDato: LocalDate,
        connection: Connection
    ): PerioderMed11_17Response {
        return connection.prepareStatement(selectVedtakMedTidsbegrensningSql).use { preparedStatement ->
            preparedStatement.setString(1, personId)
            preparedStatement.setDate(2, Date.valueOf(fraOgMedDato))
            preparedStatement.setDate(3, Date.valueOf(tilOgMedDato))

            val resultSet = preparedStatement.executeQuery()

            val perioder = resultSet.map { row ->
                PeriodeMed11_17(
                    fraOgMedDato = row.getDate("fra_dato").toLocalDate(),
                    tilOgMedDato = getNullableDate(row.getDate("til_dato")),
                    aktivitetsfase = row.getString("aktfasenavn")
                )
            }
            PerioderMed11_17Response(perioder)
        }
    }

    private fun getNullableDate(date: Date?): LocalDate? {
        if (date == null) return null
        return date.toLocalDate()
    }
}
