package no.nav.aap.arenaoppslag.database

import no.nav.aap.arenaoppslag.database.DbDato.fraDato
import no.nav.aap.arenaoppslag.kontrakt.intern.Status
import no.nav.aap.arenaoppslag.modeller.ArenaVedtak
import no.nav.aap.arenaoppslag.modeller.ArenaVedtakMedMaxdato
import no.nav.aap.arenaoppslag.modeller.ArenaVedtakRad
import no.nav.aap.arenaoppslag.modeller.Periode
import no.nav.aap.arenaoppslag.modeller.PersonId
import no.nav.aap.arenaoppslag.modeller.SakId
import no.nav.aap.arenaoppslag.modeller.VedtakStatus
import org.intellij.lang.annotations.Language
import org.jetbrains.annotations.TestOnly
import java.sql.Connection
import java.sql.Date
import java.sql.ResultSet
import java.time.LocalDate
import javax.sql.DataSource

class VedtakRepository(private val dataSource: DataSource) {

    fun hentSisteLøpendeVedtakDetalj(personId: PersonId): ArenaVedtakMedMaxdato? {
        return dataSource.connection.use { connection ->
            connection.prepareStatement(selectSisteVedtakSomIkkeErStansetOgPåvirkerKvote).use { preparedStatement ->
                preparedStatement.setInt(1, personId.id)
                val resultSet = preparedStatement.executeQuery()
                if (resultSet.next()) mapperForArenaVedtakDetalj(resultSet) else null
            }
        }
    }

    fun hentVedtakStatuser(fodselsnr: String): List<VedtakStatus> {
        return dataSource.connection.use { con ->
            selectVedtakStatuser(fodselsnr, con)
        }
    }

    @TestOnly
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

    fun hentVedtakForSak(saksId: SakId): List<ArenaVedtakRad> {
        return dataSource.connection.use { con ->
            selectVedtakForSak(saksId, con)
        }
    }

    fun hentForsteInnvilgetVedtakForSak(saksId: SakId): ArenaVedtakRad? {
        return dataSource.connection.use { con ->
            selectForsteInnvilgetVedtakForSak(saksId, con)
        }
    }

    fun hentGjeldende115VedtakForSakPåDato(saksId: SakId, dato: LocalDate): ArenaVedtakRad? {
        return dataSource.connection.use { con ->
            selectGjeldende115VedtakPåDato(saksId, dato, con)
        }
    }

    fun hentGjeldendeAapVedtakForSakPåDato(saksId: SakId, dato: LocalDate): ArenaVedtakRad? {
        return dataSource.connection.use { con ->
            selectGjeldendeAapVedtakPåDato(saksId, dato, con)
        }
    }

    companion object {

        @Language("OracleSql")
        internal val selectSisteVedtakSomIkkeErStansetOgPåvirkerKvote = """
            WITH nyeste_vedtak AS (
                SELECT rettighetkode, utfallkode, aar, sak_id, vedtak_id, lopenrvedtak, vedtaktypekode, aktfasekode, vedtakstatuskode, fra_dato, til_dato FROM (
                    SELECT v.sak_id,
                        v.vedtak_id,
                        v.vedtaktypekode,
                        v.aktfasekode,
                        v.fra_dato,
                        v.til_dato,
                        v.vedtakstatuskode,
                        v.lopenrvedtak,
                        v.aar,
                        v.rettighetkode,
                        v.utfallkode,
                        ROW_NUMBER() OVER (PARTITION BY v.person_id ORDER BY v.til_dato DESC NULLS FIRST, v.vedtak_id DESC) as rn
                    FROM vedtak v
                    WHERE v.person_id = ?
                        AND v.rettighetkode = 'AAP'
                        AND v.utfallkode = 'JA'
                        AND v.vedtakstatuskode IN ('IVERK','AVSLU')
                        AND v.vedtaktypekode IN ('O', 'E', 'G')
                        AND v.aktfasekode IN ('UA', 'AU')
                        -- ignorer ugyldiggjorte vedtak og etterregistrerte vedtak:
                        AND v.fra_dato IS NOT NULL
                        AND NOT ((v.fra_dato IS NOT NULL and v.til_dato IS NOT NULL) AND v.fra_dato > v.til_dato)
                ) WHERE rn = 1
            )
            SELECT nv.sak_id, nv.aar, nv.vedtak_id, nv.lopenrvedtak, nv.aktfasekode, nv.vedtaktypekode, nv.fra_dato, 
            nv.til_dato, nv.vedtakstatuskode, nv.rettighetkode, nv.utfallkode, vmd.max_dato, vmd.max_unntak_dato
            FROM nyeste_vedtak nv
                JOIN v_vedtak_maxdato vmd ON vmd.vedtak_id = nv.vedtak_id
            FETCH FIRST 1 ROW ONLY
        """.trimIndent()

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
            aktivitetsfaseKode = row.getString("aktfasekode"),
        )

        fun mapperForArenaVedtakDetalj(row: ResultSet) = ArenaVedtakMedMaxdato(
            sakId = row.getInt("sak_id"),
            opprettetAar = row.getInt("aar"),
            lopenrvedtak = row.getInt("lopenrvedtak"),
            vedtakId = row.getInt("vedtak_id"),
            aktivitetsfaseKode = row.getString("aktfasekode"),
            vedtaktypeKode = row.getString("vedtaktypekode"),
            statusKode = row.getString("vedtakstatuskode"),
            rettighetkode = row.getString("rettighetkode"),
            utfallkode = row.getString("utfallkode"),
            tilDato = row.getDate("til_dato")?.toLocalDate(),
            fraOgMed = row.getDate("fra_dato")?.toLocalDate(),
            maxdatoUnntak = row.getDate("max_unntak_dato")?.toLocalDate(),
            maxdatoOrdinaer = row.getDate("max_dato")?.toLocalDate(),
        )

        @Language("OracleSql")
        internal val selectAlleVedtakForFnr = """
        SELECT vedtakstatuskode, vedtaktypekode, sak_id, fra_dato, til_dato, rettighetkode, aktfasekode, utfallkode
          FROM vedtak
         WHERE person_id = 
               (SELECT person_id 
                  FROM person 
                 WHERE fodselsnr = ?) 
        """.trimIndent()

        @Language("OracleSql")
        internal val selectAlleVedtakForPerson = """
        SELECT vedtakstatuskode, vedtaktypekode, sak_id, fra_dato, til_dato, rettighetkode, aktfasekode, utfallkode
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
        """.trimIndent()

        private fun selectVedtakForSak(sakId: SakId, connection: Connection): List<ArenaVedtakRad> {
            connection.createParameterizedQuery(selectVedtakForSak).use { preparedStatement ->
                preparedStatement.setInt(1, sakId.id)
                val resultSet = preparedStatement.executeQuery()
                return resultSet.map { mapperForArenaVedtakRad(it) }.toList()
            }
        }

        @Language("OracleSql")
        private val selectForsteInnvilgetVedtakForSak = """
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
           AND v.utfallkode = 'JA'
         ORDER BY v.fra_dato ASC
         FETCH FIRST 1 ROW ONLY
        """.trimIndent()

        private fun selectForsteInnvilgetVedtakForSak(sakId: SakId, connection: Connection): ArenaVedtakRad? {
            connection.createParameterizedQuery(selectForsteInnvilgetVedtakForSak).use { preparedStatement ->
                preparedStatement.setInt(1, sakId.id)
                val resultSet = preparedStatement.executeQuery()
                return if (resultSet.next()) mapperForArenaVedtakRad(resultSet) else null
            }
        }

        @Language("OracleSql")
        private val selectGjeldende115VedtakPåDato = """
        SELECT v.vedtak_id, v.lopenrvedtak, v.vedtakstatuskode, vs.vedtakstatusnavn, v.vedtaktypekode, vt.vedtaktypenavn,
               v.fra_dato, v.til_dato, v.rettighetkode, rt.rettighetnavn, v.utfallkode, v.begrunnelse,
               v.brukerid_ansvarlig, v.brukerid_beslutter, v.vedtak_id_relatert,
               a.aktfasekode, a.aktfasenavn
          FROM vedtak v
          LEFT JOIN vedtaktype vt ON vt.vedtaktypekode = v.vedtaktypekode
          LEFT JOIN vedtakstatus vs ON v.vedtakstatuskode = vs.vedtakstatuskode
          LEFT JOIN aktivitetfase a ON a.aktfasekode = v.aktfasekode
          LEFT JOIN rettighettype rt ON rt.rettighetkode = v.rettighetkode
         WHERE v.sak_id = ?
           AND v.rettighetkode = 'AA115'
           AND v.vedtaktypekode IN ('O', 'E', 'G')
           AND v.utfallkode = 'JA'
           AND v.vedtakstatuskode = 'IVERK'
           AND v.fra_dato <= ?
           AND (v.til_dato IS NULL OR v.til_dato >= ?)
         ORDER BY v.fra_dato DESC, v.vedtak_id DESC
         FETCH FIRST 1 ROW ONLY
        """.trimIndent()

        private fun selectGjeldende115VedtakPåDato(
            sakId: SakId,
            dato: LocalDate,
            connection: Connection,
        ): ArenaVedtakRad? {
            connection.createParameterizedQuery(selectGjeldende115VedtakPåDato).use { preparedStatement ->
                val datoAsDate = Date.valueOf(dato)
                preparedStatement.setInt(1, sakId.id)
                preparedStatement.setDate(2, datoAsDate)
                preparedStatement.setDate(3, datoAsDate)
                val resultSet = preparedStatement.executeQuery()
                return if (resultSet.next()) mapperForArenaVedtakRad(resultSet) else null
            }
        }

        @Language("OracleSql")
        private val selectGjeldendeAapVedtakPåDato = """
        SELECT v.vedtak_id, v.lopenrvedtak, v.vedtakstatuskode, vs.vedtakstatusnavn, v.vedtaktypekode, vt.vedtaktypenavn,
               v.fra_dato, v.til_dato, v.rettighetkode, rt.rettighetnavn, v.utfallkode, v.begrunnelse,
               v.brukerid_ansvarlig, v.brukerid_beslutter, v.vedtak_id_relatert,
               a.aktfasekode, a.aktfasenavn
          FROM vedtak v
          LEFT JOIN vedtaktype vt ON vt.vedtaktypekode = v.vedtaktypekode
          LEFT JOIN vedtakstatus vs ON v.vedtakstatuskode = vs.vedtakstatuskode
          LEFT JOIN aktivitetfase a ON a.aktfasekode = v.aktfasekode
          LEFT JOIN rettighettype rt ON rt.rettighetkode = v.rettighetkode
         WHERE v.sak_id = ?
           AND v.rettighetkode = 'AAP'
           AND v.vedtaktypekode IN ('O', 'E', 'G')
           AND v.utfallkode = 'JA'
           AND v.vedtakstatuskode = 'IVERK'
           AND v.aktfasekode IN ('UA', 'AU')
           AND v.fra_dato <= ?
           AND (v.til_dato IS NULL OR v.til_dato >= ?)
         ORDER BY v.fra_dato DESC, v.vedtak_id DESC
         FETCH FIRST 1 ROW ONLY
        """.trimIndent()

        private fun selectGjeldendeAapVedtakPåDato(
            sakId: SakId,
            dato: LocalDate,
            connection: Connection,
        ): ArenaVedtakRad? {
            connection.createParameterizedQuery(selectGjeldendeAapVedtakPåDato).use { preparedStatement ->
                val datoAsDate = Date.valueOf(dato)
                preparedStatement.setInt(1, sakId.id)
                preparedStatement.setDate(2, datoAsDate)
                preparedStatement.setDate(3, datoAsDate)
                val resultSet = preparedStatement.executeQuery()
                return if (resultSet.next()) mapperForArenaVedtakRad(resultSet) else null
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

