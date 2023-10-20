package arenaoppslag.fellesordningen

import arenaoppslag.dao.map
import java.sql.Date
import java.time.LocalDate
import javax.sql.DataSource

class FellesordningenDao(private val dataSource: DataSource) {

    private val selectVedtakMedTidsbegrensningSql = """
        SELECT til_dato, fra_dato 
          FROM vedtak 
         WHERE utfallkode = 'JA' 
           AND person_id = 
               (SELECT person_id 
                  FROM person 
                 WHERE fodselsnr = ?) 
           AND (til_dato >= ? OR til_dato IS NULL) 
           AND fra_dato < ?
    """

    fun selectVedtakMedTidsbegrensning(personId: String, datoForØnsketUttakForAFP: LocalDate): VedtakResponse {
        return dataSource.connection.use { connection ->
            connection.prepareStatement(selectVedtakMedTidsbegrensningSql).use { preparedStatement ->
                preparedStatement.setString(1, personId)
                preparedStatement.setDate(2, Date.valueOf(datoForØnsketUttakForAFP.minusYears(3)))
                preparedStatement.setDate(3, Date.valueOf(datoForØnsketUttakForAFP))

                val resultSet = preparedStatement.executeQuery()

                val perioder = resultSet.map { row ->
                    VedtakPeriode(
                        fraDato = row.getDate("fra_dato").toLocalDate(),
                        tilDato = getNullableDate(row.getDate("til_dato")),
                    )
                }.toList()

                VedtakResponse(personId, perioder)
            }
        }
    }

    private fun getNullableDate(date: Date?): LocalDate? {
        if(date == null) return null
        return date.toLocalDate()
    }
}
