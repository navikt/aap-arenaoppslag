package no.nav.aap.arenaoppslag.database

import no.nav.aap.arenaoppslag.modeller.ArenaSak
import no.nav.aap.arenaoppslag.modeller.ArenaSakOppsummering
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


    fun hentSakerForPerson(personidentifikatorerForPerson: Set<String>): List<ArenaSakOppsummering> {
        if (personidentifikatorerForPerson.isEmpty()) return emptyList()
        return dataSource.connection.use { con ->
            selectSakerForPersoner(personidentifikatorerForPerson, con)
        }
    }

    companion object {
        private const val FNR_LISTE_TOKEN = "?:fodselsnummer"

        private fun queryMedFodselsnummerListe(baseQuery: String, fodselsnumre: Set<String>): String {
            // Oracle støtter ikke listeparametere i PreparedStatement, så vi interpolerer direkte
            val allePersonensFodselsnummer = fodselsnumre.joinToString(separator = ",") { "'$it'" }
            return baseQuery.replace(FNR_LISTE_TOKEN, allePersonensFodselsnummer)
        }

        fun selectSakerForPersoner(fodselsnumre: Set<String>, connection: Connection): List<ArenaSakOppsummering> {
            val query = queryMedFodselsnummerListe(selectSakerMedAntallVedtakForFnrListe, fodselsnumre)
            // Er i teorien unsafe, men data kommer fra PDL så SQL injection risken er lav
            connection.prepareStatement(query).use { preparedStatement ->
                val resultSet = preparedStatement.executeQuery()
                return resultSet.map { row -> mapperForArenaSakKontrakt(row) }
            }
        }

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
            statusnavn = row.getString("sakstatusnavn"),
            registrertDato = row.getTimestamp("reg_dato").toLocalDateTime(),
            avsluttetDato = row.getTimestamp("dato_avsluttet")?.toLocalDateTime(),
            person = ArenaSakPerson(
                personId = row.getInt("person_id"),
                fodselsnummer = row.getString("fodselsnr"),
                fornavn = row.getString("fornavn"),
                etternavn = row.getString("etternavn"),
            )
        )

        private fun mapperForArenaSakKontrakt(row: ResultSet) = ArenaSakOppsummering(
            sakId = row.getString("sak_id"),
            lopenummer = row.getInt("lopenrsak"),
            aar = row.getInt("aar"),
            antallVedtak = row.getInt("antall_vedtak"),
            sakstype = row.getString("sakstypenavn"),
            regDato = row.getDate("reg_dato").toLocalDate(),
            avsluttetDato = row.getDate("dato_avsluttet")?.toLocalDate(),
            statusnavn = row.getString("sakstatusnavn"),
            statuskode = row.getString("sakstatuskode"),
        )

        @Language("OracleSql")
        internal val selectSakMedSaksId = """
            SELECT sak.sak_id, sak.aar, sak.sakstatuskode, sakstatus.sakstatusnavn, sak.lopenrsak, person.person_id, 
                person.fornavn, person.etternavn, person.fodselsnr, sak.reg_dato, sak.dato_avsluttet
            FROM SAK
            LEFT JOIN person ON person.person_id = sak.objekt_id
            LEFT JOIN sakstatus ON sak.sakstatuskode = sakstatus.sakstatuskode
            WHERE SAK_ID = ? AND TABELLNAVNALIAS='PERS'
        """.trimIndent()

        @Language("OracleSql")
        internal val selectSakerMedAntallVedtakForFnrListe = """
            SELECT sak.sak_id, sak.aar, sak.lopenrsak, sakstype.sakstypenavn, sak.reg_dato, sak.dato_avsluttet,
                sak.sakstatuskode, sakstatus.sakstatusnavn, COUNT(vedtak.vedtak_id) AS antall_vedtak
            FROM SAK
            JOIN person ON person.person_id = sak.objekt_id
            JOIN sakstype ON sakstype.sakskode = sak.sakskode
            LEFT JOIN sakstatus ON sak.sakstatuskode = sakstatus.sakstatuskode
            LEFT JOIN vedtak ON vedtak.sak_id = sak.sak_id
            WHERE person.fodselsnr IN ($FNR_LISTE_TOKEN) AND sak.tabellnavnalias = 'PERS'
            GROUP BY sak.sak_id, sak.aar, sak.lopenrsak, sakstype.sakstypenavn, sak.reg_dato, sak.dato_avsluttet,
                sak.sakstatuskode, sakstatus.sakstatusnavn
        """.trimIndent()
    }

}