package no.nav.aap.arenaoppslag.database

import no.nav.aap.arenaoppslag.database.DbDato.fraDato
import no.nav.aap.arenaoppslag.kontrakt.intern.Status
import no.nav.aap.arenaoppslag.kontrakt.modeller.Periode
import no.nav.aap.arenaoppslag.modeller.ArenaVedtak
import no.nav.aap.arenaoppslag.modeller.VedtakStatus
import org.intellij.lang.annotations.Language
import org.jetbrains.annotations.TestOnly
import java.sql.Connection
import java.sql.ResultSet
import javax.sql.DataSource

class VedtakRepository(private val dataSource: DataSource) {

    fun hentVedtakStatuser(fnr: String): List<VedtakStatus> {
        return dataSource.connection.use { con ->
            selectVedtakStatuser(fnr, con)
        }
    }

    fun hentVedtak(fnr: String): List<ArenaVedtak> {
        return dataSource.connection.use { con ->
            selectVedtak(fnr, con)
        }
    }

    companion object {

        @TestOnly
        fun selectVedtak(fodselsnummer: String, connection: Connection): List<ArenaVedtak> {
            connection.prepareStatement(selectAlleVedtakForFnr).use { preparedStatement ->
                preparedStatement.setString(1, fodselsnummer)
                val resultSet = preparedStatement.executeQuery()
                return resultSet.map { row -> mapperForArenaVedtak(row) }.toList()
            }
        }

        fun mapperForArenaVedtak(row: ResultSet) = ArenaVedtak(
            row.getString("sak_id"),
            row.getString("vedtakstatuskode"),
            row.getString("vedtaktypekode"),
            fraDato(row.getDate("fra_dato")),
            tilDato = fraDato(row.getDate("til_dato")),
            rettighetkode = row.getString("rettighetkode"),
            utfallkode = row.getString("utfallkode"),
        )

        @Language("OracleSql")
        internal val selectAlleVedtakForFnr = """
        SELECT vedtakstatuskode, vedtaktypekode, sak_id, fra_dato, til_dato, rettighetkode, utfallkode
          FROM vedtak
         WHERE person_id = 
               (SELECT person_id 
                  FROM person 
                 WHERE fodselsnr = ?) 
        """.trimIndent()

        @Language("OracleSql")
        private val selectVedtakForFnr = """
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

        fun selectVedtakStatuser(fodselsnr: String, connection: Connection): List<VedtakStatus> {
            connection.prepareStatement(selectVedtakForFnr).use { preparedStatement ->
                preparedStatement.setString(1, fodselsnr)
                val resultSet = preparedStatement.executeQuery()
                return resultSet.map { row -> mapperForVedtakStatus(row) }.toList()
            }
        }

        private fun mapperForVedtakStatus(row: ResultSet) = VedtakStatus(
            row.getString("sak_id"),
            Status.fraStrengverdi(row.getString("vedtakstatuskode")),
            Periode(
                fraOgMedDato = fraDato(row.getDate("fra_dato")),
                tilOgMedDato = fraDato(row.getDate("til_dato"))
            )
        )
    }

}
