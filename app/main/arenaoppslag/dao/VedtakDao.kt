package arenaoppslag.dao

import arenaoppslag.dto.FellesOrdningDTO
import arenaoppslag.modell.Vedtak
import java.sql.Date
import java.time.LocalDate
import javax.sql.DataSource

class VedtakDao(private val dataSource: DataSource) {

    private val hentVedtakForEnPersonSql = """
       SELECT * FROM vedtak WHERE utfallkode IS NOT NULL AND person_id = 
       (SELECT person_id FROM person WHERE fodselsnr = ?)
    """

    private val selectVedtakMedTidsbegrensningSql = """
        SELECT fodselsnr, utfallkode, til_dato, fra_dato FROM vedtak, VED WHERE utfallkode IS NOT NULL AND person_id = 
       (SELECT person_id FROM person WHERE fodselsnr = ?) AND (til_dato >= ? OR til_dato IS NULL) AND fra_dato < ?
    """ //TODO: sjekk om vi ønsker resultat hvor utfallkode er null (vi antar at det betyr ikke fattet vedtak)


    fun selectAlleVedtak(fnr: String): List<Vedtak> {
        return dataSource.connection.use { connection ->
            connection.prepareStatement(hentVedtakForEnPersonSql).use { preparedStatement ->  
                preparedStatement.setString(1, fnr)

                val resultSet = preparedStatement.executeQuery()

                resultSet.map { row ->
                    Vedtak(
                        personId = fnr,
                        utfallkode = row.getString("utfallkode"),
                        datoMottatt = row.getDate("dato_mottatt").toLocalDate(),
                        vedtakId = row.getInt("vedtak_id"),
                        sakId = row.getInt("sak_id"),
                        vedtakstatuskode = row.getString("vedtakstatuskode"),
                        vedtaktypekode = row.getString("vedtakstypekode"),
                        modDato = row.getDate("mod_dato").toLocalDate(),
                        modUser = row.getString("mod_user"),
                        regDato = row.getDate("reg_dato").toLocalDate(),
                        regUser = row.getString("reg_user"),
                        tilDato = row.getDate("til_dato").toLocalDate(),
                        fraDato = row.getDate("fra_dato").toLocalDate()
                    )
                }.toList()
            }
        }
    }

    fun selectVedtakMedTidsbegrensning(fnr:String, datoForØnsketUttakForAFP: LocalDate){
        return dataSource.connection.use { connection ->
            connection.prepareStatement(hentVedtakForEnPersonSql).use { preparedStatement ->
                preparedStatement.setString(1, fnr)
                preparedStatement.setDate(2, Date.valueOf(datoForØnsketUttakForAFP.minusYears(3)))
                preparedStatement.setDate(3, Date.valueOf(datoForØnsketUttakForAFP) )

                val resultSet = preparedStatement.executeQuery()

                resultSet.map { row->
                    FellesOrdningDTO(
                        personId = fnr,
                        fraDato = row.getDate("fra_dato").toLocalDate(),
                        tilDato = row.getDate("til_dato").toLocalDate(),
                )}
            }
    }

}