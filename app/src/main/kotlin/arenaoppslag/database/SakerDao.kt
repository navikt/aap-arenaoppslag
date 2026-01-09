package arenaoppslag.database

import arenaoppslag.database.DbDato.fraDato
import no.nav.aap.arenaoppslag.kontrakt.intern.SakStatus
import no.nav.aap.arenaoppslag.kontrakt.intern.Status
import org.intellij.lang.annotations.Language
import java.sql.Connection
import java.sql.ResultSet
import no.nav.aap.arenaoppslag.kontrakt.modeller.Periode as KontraktPeriode

object SakerDao {

    @Language("OracleSql")
    private val selectSaksIdByFnr = """
        SELECT vedtakstatuskode, sak_id, fra_dato, til_dato
          FROM vedtak
         WHERE person_id = 
               (SELECT person_id 
                  FROM person 
                 WHERE fodselsnr = ?) 
           AND rettighetkode = 'AAP'
           AND vedtaktypekode IN ('O', 'E', 'G', 'S')
           AND (fra_dato <= til_dato OR til_dato IS NULL)
    """.trimIndent()

    fun selectSaker(personidentifikator: String, connection: Connection): List<SakStatus> {
        connection.prepareStatement(selectSaksIdByFnr)
            .use { preparedStatement ->
                preparedStatement.setString(1, personidentifikator)
                val resultSet = preparedStatement.executeQuery()
                return resultSet.map { row -> mapperForSakStatus(row) }.toList()
            }
    }

    private fun mapperForSakStatus(row: ResultSet): SakStatus = SakStatus(
        row.getString("sak_id"),
        Status.entries.find { it.name == row.getString("vedtakstatuskode") }
            ?: Status.UKJENT,
        KontraktPeriode(
            fraOgMedDato = fraDato(row.getDate("fra_dato")),
            tilOgMedDato = fraDato(row.getDate("til_dato"))
        )
    )

}
