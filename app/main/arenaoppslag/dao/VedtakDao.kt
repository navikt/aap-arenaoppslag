package arenaoppslag.dao

import arenaoppslag.modell.Vedtak
import javax.sql.DataSource

class VedtakDao(private val dataSource: DataSource) {

    private val hentVedtakForEnPersonSql = """
       SELECT * FROM vedtak WHERE utfallkode IS NOT NULL AND person_id = 
       (SELECT person_id FROM person WHERE fodselsnr = ?)
    """

    fun selectAlleVedtak(fnr: String): List<Vedtak> {
        return dataSource.connection.use { connection ->
            connection.prepareStatement(hentVedtakForEnPersonSql).use { preparedStatement ->  
                preparedStatement.setString(1, fnr)

                val resultSet = preparedStatement.executeQuery()

                resultSet.map { row ->
                    Vedtak(
                        personId = fnr,
                        utfallkode = row.getString("utfallkode"),
                        datoMottatt = row.getDate("dato_mottatt").toLocalDate()
                    )
                }.toList()
            }
        }
    }

}