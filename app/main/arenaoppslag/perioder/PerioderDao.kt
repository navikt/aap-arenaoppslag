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

    private fun getNullableDate(date: Date?): LocalDate? {
        if (date == null) return null
        return date.toLocalDate()
    }
}
