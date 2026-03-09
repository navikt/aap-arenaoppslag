package no.nav.aap.arenaoppslag.database

import no.nav.aap.arenaoppslag.database.DbDato.fraDato
import no.nav.aap.arenaoppslag.kontrakt.intern.Status
import no.nav.aap.arenaoppslag.kontrakt.modeller.Periode
import no.nav.aap.arenaoppslag.modeller.ArenaVedtak
import no.nav.aap.arenaoppslag.modeller.ArenaVedtakMedFakta
import no.nav.aap.arenaoppslag.modeller.ArenaVedtakfakta
import no.nav.aap.arenaoppslag.modeller.VedtakStatus
import org.intellij.lang.annotations.Language
import org.jetbrains.annotations.TestOnly
import java.sql.Connection
import java.sql.ResultSet
import java.time.LocalDate
import javax.sql.DataSource
import kotlin.collections.map
import kotlin.use

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

    fun hentVedtakMedFaktaForSak(saksId: Int): List<ArenaVedtakMedFakta> {
        return dataSource.connection.use { con ->
            selectVedtakMedFaktaForSak(saksId, con)
                .groupBy { it.vedtakId }
                .values
                .map { vedtakMedFakta ->
                    val vedtakFakta = vedtakMedFakta.mapNotNull { it.tilArenaVedtakFakta() }
                    vedtakMedFakta
                        .first()
                        .tilArenaVedtakMedFakta(vedtakFakta)
                }
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
        private val selectVedtakMedFaktaForSak = """
        SELECT v.vedtakstatuskode, v.vedtaktypekode, v.fra_dato, v.til_dato, v.rettighetkode, v.utfallkode, vf.vedtak_id, vf.vedtakfaktakode, vf.vedtakverdi, vf.reg_dato
          FROM vedtak v
          LEFT JOIN vedtakfakta vf ON  vf.vedtak_id = v.vedtak_id
         WHERE sak_id = ?
           AND v.rettighetkode = 'AAP'
           AND v.vedtaktypekode IN ('O', 'E', 'G', 'S')
           AND (v.fra_dato <= v.til_dato OR v.til_dato IS NULL)
        """.trimIndent()

        private fun selectVedtakMedFaktaForSak(sakId: Int, connection: Connection): List<ArenaVedtakMedFaktaRow> {
            connection.prepareStatement(selectVedtakMedFaktaForSak).use { preparedStatement ->
                preparedStatement.setInt(1, sakId)
                val resultSet = preparedStatement.executeQuery()
                return resultSet.map { ArenaVedtakMedFaktaRow.fromResultRow(it) }.toList()
            }
        }
    }


    private data class ArenaVedtakMedFaktaRow(
        val statusKode: String,
        val vedtaktypeKode: String?,
        val fraOgMed: LocalDate?,
        val tilDato: LocalDate?,
        val rettighetkode: String,
        val utfallkode: String?,
        val vedtakId: Int,
        val vedtakfaktakode: String?,
        val vedtakfaktakodeverdi: String?,
        val vedtakfaktakoderegistrertDato: LocalDate?,
    ) {
        companion object {
            fun fromResultRow(row: ResultSet) = ArenaVedtakMedFaktaRow(
                statusKode = row.getString("vedtakstatuskode"),
                vedtaktypeKode = row.getString("vedtaktypekode"),
                fraOgMed = fraDato(row.getDate("fra_dato")),
                tilDato = fraDato(row.getDate("til_dato")),
                rettighetkode = row.getString("rettighetkode"),
                utfallkode = row.getString("utfallkode"),
                vedtakId = row.getInt("vedtak_id"),
                vedtakfaktakode = row.getString("vedtakfaktakode"),
                vedtakfaktakodeverdi = row.getString("vedtakverdi"),
                vedtakfaktakoderegistrertDato = row.getDate("reg_dato")?.toLocalDate()
            )
        }

        fun tilArenaVedtakMedFakta(fakta: List<ArenaVedtakfakta>) =
            ArenaVedtakMedFakta(
                statusKode = statusKode,
                vedtaktypeKode = vedtaktypeKode,
                fraOgMed = fraOgMed,
                tilDato = tilDato,
                rettighetkode = rettighetkode,
                utfallkode = utfallkode,
                fakta = fakta
            )

        fun tilArenaVedtakFakta(): ArenaVedtakfakta? {
            if (vedtakfaktakode == null || vedtakfaktakoderegistrertDato == null) {
                return null
            }
            return ArenaVedtakfakta(
                vedtakId = vedtakId,
                kode = vedtakfaktakode,
                verdi = vedtakfaktakodeverdi,
                registrertDato = vedtakfaktakoderegistrertDato
            )
        }

    }
}
