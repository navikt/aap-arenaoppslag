package no.nav.aap.arenaoppslag.database

import no.nav.aap.arenaoppslag.modeller.ArenaSak
import no.nav.aap.arenaoppslag.modeller.ArenaSakPerson
import org.intellij.lang.annotations.Language
import java.sql.Connection
import java.sql.ResultSet
import javax.sql.DataSource
import kotlin.use

class SakRepository(private val dataSource: DataSource) {

    fun hentSak(saksId: Int): ArenaSak? {
        return dataSource.connection.use { con ->
            selectSakMedId(saksId, con)
        }
    }

    companion object {
        fun selectSakMedId(saksid: Int, connection: Connection): ArenaSak? {
            connection.prepareStatement(selectSakMedSaksId).use { preparedStatement ->
                preparedStatement.setInt(1, saksid)
                val resultSet = preparedStatement.executeQuery()
                val saker = resultSet.map { row -> mapperForArenaSak(row) }

                if (saker.isEmpty()) {
                    return null
                }

                return saker.first()
            }
        }

        fun mapperForArenaSak(row: ResultSet) = ArenaSak(
            sakId = row.getString("sak_id"),
            opprettetAar = row.getInt("aar"),
            lopenr = row.getInt("lopenrsak"),
            statuskode = row.getString("sakstatuskode"),
            registrertDato = row.getTimestamp("reg_dato").toLocalDateTime(),
            avsluttetDato = row.getTimestamp("dato_avsluttet")?.toLocalDateTime(),
            person = ArenaSakPerson(
                personId = row.getInt("person_id"),
                fodselsnummer = row.getString("fodselsnr"),
                fornavn = row.getString("fornavn"),
                etternavn = row.getString("etternavn"),
            )
        )

        @Language("OracleSql")
        internal val selectSakMedSaksId = """
            SELECT sak.sak_id, sak.aar, sak.sakstatuskode, sak.lopenrsak, person.person_id, person.fornavn, person.etternavn, person.fodselsnr, sak.reg_dato, sak.dato_avsluttet
            FROM SAK
            LEFT JOIN person ON person.person_id = sak.objekt_id
            WHERE SAK_ID = ? AND TABELLNAVNALIAS='PERS'
        """.trimIndent()
    }

}