package no.nav.aap.arenaoppslag.database

import no.nav.aap.arenaoppslag.database.DbDato.fraDato
import no.nav.aap.arenaoppslag.kontrakt.intern.ArenaSak
import no.nav.aap.arenaoppslag.kontrakt.intern.SakStatus
import no.nav.aap.arenaoppslag.kontrakt.intern.Status
import no.nav.aap.arenaoppslag.kontrakt.modeller.Periode
import org.intellij.lang.annotations.Language
import org.jetbrains.annotations.TestOnly
import java.sql.Connection
import java.sql.ResultSet
import javax.sql.DataSource


class SakRepository(private val dataSource: DataSource) {

    fun hentSakStatuser(personidentifikator: String): List<SakStatus> {
        return dataSource.connection.use { con ->
            selectSakStatuser(personidentifikator, con)
        }
    }

    fun hentSaker(personidentifikator: String): List<ArenaSak> {
        return dataSource.connection.use { con ->
            selectSak(personidentifikator, con)
        }
    }

    companion object {

        @TestOnly
        fun selectSak(personidentifikator: String, connection: Connection): List<ArenaSak> {
            connection.prepareStatement(selectAlleSakerByFnr).use { preparedStatement ->
                preparedStatement.setString(1, personidentifikator)
                val resultSet = preparedStatement.executeQuery()
                return resultSet.map { row -> mapperForArenasak(row) }.toList()
            }
        }

        fun mapperForArenasak(row: ResultSet): ArenaSak = ArenaSak(
            row.getString("sak_id"),
            row.getString("vedtakstatuskode"),
            row.getString("vedtaktypekode"),
            fraDato(row.getDate("fra_dato")),
            tilDato = fraDato(row.getDate("til_dato")),
            rettighetkode = row.getString("rettighetkode")
        )

        @Language("OracleSql")
        internal val selectAlleSakerByFnr = """
        SELECT vedtakstatuskode, vedtaktypekode, sak_id, fra_dato, til_dato, rettighetkode
          FROM vedtak
         WHERE person_id = 
               (SELECT person_id 
                  FROM person 
                 WHERE fodselsnr = ?) 
    """.trimIndent()

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

        fun selectSakStatuser(personidentifikator: String, connection: Connection): List<SakStatus> {
            connection.prepareStatement(selectSaksIdByFnr).use { preparedStatement ->
                preparedStatement.setString(1, personidentifikator)
                val resultSet = preparedStatement.executeQuery()
                return resultSet.map { row -> mapperForSakStatus(row) }.toList()
            }
        }

        private fun mapperForSakStatus(row: ResultSet): SakStatus = SakStatus(
            row.getString("sak_id"),
            Status.entries.find { it.name == row.getString("vedtakstatuskode") }
                ?: Status.UKJENT,
            Periode(
                fraOgMedDato = fraDato(row.getDate("fra_dato")),
                tilOgMedDato = fraDato(row.getDate("til_dato"))
            )
        )

    }

}
