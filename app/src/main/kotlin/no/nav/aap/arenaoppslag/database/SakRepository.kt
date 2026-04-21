package no.nav.aap.arenaoppslag.database

import no.nav.aap.arenaoppslag.modeller.ArenaSak
import no.nav.aap.arenaoppslag.modeller.ArenaSakOppsummering
import no.nav.aap.arenaoppslag.modeller.ArenaSakPerson
import no.nav.aap.arenaoppslag.modeller.Maksdatolinje
import org.intellij.lang.annotations.Language
import java.sql.Connection
import java.sql.ResultSet
import javax.sql.DataSource

class SakRepository(private val dataSource: DataSource) {

    fun hentSak(saksId: Int): ArenaSak? {
        return dataSource.connection.use { con ->
            selectSakMedId(saksId, con)
        }
    }


    fun hentSakerForPersnNummere(fodselsnumre: Set<String>): List<ArenaSakOppsummering> {
        if (fodselsnumre.isEmpty()) return emptyList()
        return dataSource.connection.use { con ->
            selectSakerForPersoner(fodselsnumre, con)
        }
    }

    fun finnMaksdatoer(sakidliste: List<Int>): List<Maksdatolinje> {
        if (sakidliste.isEmpty()) return emptyList()
        return dataSource.connection.use { con ->
            vedtakMedNyesteMaxdatoForSakerMedUnntak(sakidliste, con)
        }
    }

    companion object {
        private const val FNR_LISTE_TOKEN = "?:fodselsnummer"
        private const val SAK_LISTE_TOKEN = "?:sak_id"

        private fun vedtakMedNyesteMaxdatoForSakerMedUnntak(sakidliste: List<Int>, connection: Connection): List<Maksdatolinje> {
            val alleSakId = sakidliste.joinToString(separator = ",") { "'$it'" }
            val query = selectVedtakMedNyesteMaxdatoForSakerMedUnntak.replace(SAK_LISTE_TOKEN, alleSakId)

            connection.prepareStatement(query).use { preparedStatement ->
                val resultSet = preparedStatement.executeQuery()
                // tom liste om ingen rader blir funnet
                return resultSet.map { row -> mapperForMaksdatolinje(row) }
            }
        }

        fun mapperForMaksdatolinje(row: ResultSet) =
            Maksdatolinje(
                vedtakId = row.getInt("vedtak_id"),
                sakId = row.getInt("sak_id"),
                aktfaseKode = row.getString("aktfasekode"),
                maxUnntakDato = row.getDate("max_unntak_dato")?.toLocalDate()
            )

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
                COUNT(vedtak.vedtak_id) AS antall_vedtak
            FROM SAK
            JOIN person ON person.person_id = sak.objekt_id
            JOIN sakstype ON sakstype.sakskode = sak.sakskode
            LEFT JOIN vedtak ON vedtak.sak_id = sak.sak_id
            WHERE person.fodselsnr IN ($FNR_LISTE_TOKEN) AND sak.tabellnavnalias = 'PERS'
            GROUP BY sak.sak_id, sak.aar, sak.lopenrsak, sakstype.sakstypenavn, sak.reg_dato, sak.dato_avsluttet
        """.trimIndent()

        @Language("OracleSql")
        internal val selectVedtakMedNyesteMaxdatoForSakerMedUnntak = """
            -- Hent først nyeste vedtak hvor dato for forlengelse etter 11-12 er satt, for hver av sakene 
            WITH nyeste_vedtak AS (
                SELECT sak_id, vedtak_id, vedtaktypekode, aktfasekode FROM (
                    SELECT v.sak_id, 
                        v.vedtak_id,
                        v.vedtaktypekode,
                        v.aktfasekode,
                        ROW_NUMBER() OVER (PARTITION BY v.sak_id ORDER BY TO_DATE(vf.vedtakverdi, 'DD-MM-YYYY') DESC, v.vedtak_id DESC) as rn
                    FROM vedtak v
                        JOIN SAK s on s.sak_id = v.sak_id
                        JOIN VEDTAKFAKTA vf on v.vedtak_id = vf.vedtak_id
                    WHERE s.SAKSTATUSKODE = 'AKTIV' -- kun aktive saker teller med
                        AND v.sak_id in ($SAK_LISTE_TOKEN)
                        AND v.rettighetkode = 'AAP'
                        AND vf.vedtakfaktakode = 'AAPVILKUNN' 
                        AND vf.vedtakverdi IS NOT NULL -- er bare satt dersom 11-12 unntak er innvilget
                ) WHERE rn = 1
            )
            SELECT nv.sak_id, nv.vedtak_id, nv.aktfasekode, vmd.max_unntak_dato
            FROM nyeste_vedtak nv
            JOIN v_vedtak_maxdato vmd ON vmd.vedtak_id = nv.vedtak_id
            WHERE nv.vedtaktypekode != 'S' -- stansede vedtak sin maxdato er ikke meningsfull
        """.trimIndent()
    }

}