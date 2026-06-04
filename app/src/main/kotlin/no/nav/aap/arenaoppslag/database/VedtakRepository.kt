package no.nav.aap.arenaoppslag.database

import no.nav.aap.arenaoppslag.database.DbDato.fraDato
import no.nav.aap.arenaoppslag.kontrakt.intern.Status
import no.nav.aap.arenaoppslag.kontrakt.modeller.Periode
import no.nav.aap.arenaoppslag.modeller.ArenaVedtak
import no.nav.aap.arenaoppslag.modeller.ArenaVedtakRad
import no.nav.aap.arenaoppslag.modeller.PersonId
import no.nav.aap.arenaoppslag.modeller.SakId
import no.nav.aap.arenaoppslag.modeller.VedtakStatus
import no.nav.aap.arenaoppslag.tilgangsmaskin.AuthorizedPersonId
import org.intellij.lang.annotations.Language
import org.jetbrains.annotations.TestOnly
import java.sql.Connection
import java.sql.ResultSet
import javax.sql.DataSource

class VedtakRepository(private val dataSource: DataSource) {

    fun hentVedtakStatuser(fodselsnr: String): List<VedtakStatus> {
        return dataSource.connection.use { con ->
            selectVedtakStatuser(fodselsnr, con)
        }
    }

    fun hentVedtak(fnr: String): List<ArenaVedtak> {
        return dataSource.connection.use { con ->
            selectVedtak(fnr, con)
        }
    }

    fun hentVedtak(personId: PersonId): List<ArenaVedtak> {
        return dataSource.connection.use { con ->
            selectAlleVedtakForPerson(personId, con)
        }
    }

    /**
     * Context-parameter variant: only callable when [AuthorizedPersonId] is in scope,
     * meaning tilgang has already been verified. The fnr comes from the context — no
     * separate parameter needed, and no way to accidentally pass the wrong person.
     */
    context(id: AuthorizedPersonId)
    fun hentVedtak(): List<ArenaVedtak> {
        return dataSource.connection.use { con ->
            selectVedtak(id.fnr, con)
        }
    }

    fun hentVedtakForSak(saksId: SakId): List<ArenaVedtakRad> {
        return dataSource.connection.use { con ->
            selectVedtakForSak(saksId, con)
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

        fun selectAlleVedtakForPerson(personId: PersonId, connection: Connection): List<ArenaVedtak> {
            connection.prepareStatement(selectAlleVedtakForPerson).use { preparedStatement ->
                preparedStatement.setInt(1, personId.id)
                val resultSet = preparedStatement.executeQuery()
                return resultSet.map { row -> mapperForArenaVedtak(row) }.toList()
            }
        }

        fun mapperForArenaVedtak(row: ResultSet) = ArenaVedtak(
            sakId = row.getString("sak_id"),
            statusKode = row.getString("vedtakstatuskode"),
            vedtaktypeKode = row.getString("vedtaktypekode"),
            fraOgMed = fraDato(row.getDate("fra_dato")),
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
        internal val selectAlleVedtakForPerson = """
        SELECT vedtakstatuskode, vedtaktypekode, sak_id, fra_dato, til_dato, rettighetkode, utfallkode
          FROM vedtak
         WHERE person_id = ? 
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


        @Language("OracleSql")
        private val selectVedtakForSak = """
        SELECT v.vedtak_id, v.lopenrvedtak, v.vedtakstatuskode, vs.vedtakstatusnavn, v.vedtaktypekode, vt.vedtaktypenavn,
               v.fra_dato, v.til_dato, v.rettighetkode, rt.rettighetnavn, v.utfallkode, v.begrunnelse,
               v.brukerid_ansvarlig, v.brukerid_beslutter, v.vedtak_id_relatert,
               a.aktfasekode, a.aktfasenavn
          FROM vedtak v
          LEFT JOIN vedtaktype vt ON vt.vedtaktypekode = v.vedtaktypekode
          LEFT JOIN vedtakstatus vs ON v.vedtakstatuskode = vs.vedtakstatuskode
          LEFT JOIN aktivitetfase a ON a.aktfasekode = v.aktfasekode
          LEFT JOIN rettighettype rt ON rt.rettighetkode = v.rettighetkode
         WHERE sak_id = ?
           AND (fra_dato <= til_dato OR til_dato IS NULL)
        """.trimIndent()

        private fun selectVedtakForSak(sakId: SakId, connection: Connection): List<ArenaVedtakRad> {
            connection.createParameterizedQuery(selectVedtakForSak).use { preparedStatement ->
                preparedStatement.setInt(1, sakId.id)
                val resultSet = preparedStatement.executeQuery()
                return resultSet.map { mapperForArenaVedtakRad(it) }.toList()
            }
        }

        private fun mapperForArenaVedtakRad(row: ResultSet) = ArenaVedtakRad(
            vedtakId = row.getInt("vedtak_id"),
            lopenrvedtak = row.getInt("lopenrvedtak"),
            statusKode = row.getString("vedtakstatuskode"),
            statusNavn = row.getString("vedtakstatusnavn"),
            vedtaktypeKode = row.getString("vedtaktypekode"),
            vedtaktypeNavn = row.getString("vedtaktypenavn"),
            aktivitetsfaseKode = row.getString("aktfasekode"),
            aktivitetsfaseNavn = row.getString("aktfasenavn"),
            fraOgMed = fraDato(row.getDate("fra_dato")),
            tilDato = fraDato(row.getDate("til_dato")),
            rettighetkode = row.getString("rettighetkode"),
            rettighetnavn = row.getString("rettighetnavn"),
            utfallkode = row.getString("utfallkode"),
            begrunnelse = row.getString("begrunnelse"),
            saksbehandler = row.getString("brukerid_ansvarlig"),
            beslutter = row.getString("brukerid_beslutter"),
            relatertVedtak = row.getIntOrNull("vedtak_id_relatert"),
        )
    }
}
