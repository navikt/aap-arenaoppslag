package no.nav.aap.arenaoppslag.database

import no.nav.aap.arenaoppslag.modeller.ArenaSak
import no.nav.aap.arenaoppslag.modeller.ArenaSakOppsummering
import no.nav.aap.arenaoppslag.modeller.ArenaSakPerson
import no.nav.aap.arenaoppslag.modeller.Maksdatolinje
import no.nav.aap.arenaoppslag.modeller.PersonId
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

    fun hentSakerForPerson(personId: PersonId): List<ArenaSakOppsummering> {
        return dataSource.connection.use { con ->
            selectSakerForPersonId(personId, con)
        }
    }

    fun hentSakerMedMaksDatoOgVedtak(sakidliste: Set<Int>): List<Maksdatolinje> {
        if (sakidliste.isEmpty()) return emptyList()
        return dataSource.connection.use { con ->
            selectSakerMedNyesteVedtakOgMaxdato(sakidliste, con)
        }
    }

    companion object {
        private const val SAK_LISTE_TOKEN = "?:sak_id"

        private fun selectSakerMedNyesteVedtakOgMaxdato(sakidliste: Set<Int>, connection: Connection): List<Maksdatolinje> {
            val alleSakId = sakidliste.joinToString(separator = ",") { "'$it'" }
            val query = selectVedtakMedNyesteMaxdato.replace(SAK_LISTE_TOKEN, alleSakId)

            connection.prepareStatement(query).use { preparedStatement ->
                val resultSet = preparedStatement.executeQuery()
                // tom liste om ingen rader blir funnet
                return resultSet.map { row -> mapperForMaksdatolinje(row) }
            }
        }

        fun mapperForMaksdatolinje(row: ResultSet) =
            Maksdatolinje(
                sakId = row.getInt("sak_id"),
                vedtakId = row.getInt("vedtak_id"),
                aktfaseKode = row.getString("aktfasekode"),
                vedtaktypeKode = row.getString("vedtaktypekode"),
                fra = row.getDate("fra_dato")?.toLocalDate(),
                maxUnntakTil = row.getDate("max_unntak_dato")?.toLocalDate(),
                utvidetKvoteInnvilgetFra = row.getDate("unntaksdato")?.toLocalDate(),
                sakRegistrert = row.getDate("sak_registrert_dato").toLocalDate(),
                sakAvsluttet = row.getDate("sak_avsluttet_dato")?.toLocalDate(),
                sakStatus = row.getString("sak_statuskode")
            )

        fun selectSakerForPersonId(personId: PersonId, connection: Connection): List<ArenaSakOppsummering> {
            connection.prepareStatement(selectSakerMedAntallVedtakForPersonId).use { preparedStatement ->
                preparedStatement.setInt(1, personId.id)
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
        internal val selectSakerMedAntallVedtakForPersonId = """
            SELECT sak.sak_id, sak.aar, sak.lopenrsak, sakstype.sakstypenavn, sak.reg_dato, sak.dato_avsluttet,
                sak.sakstatuskode, sakstatus.sakstatusnavn, COUNT(vedtak.vedtak_id) AS antall_vedtak
            FROM SAK
            JOIN sakstype ON sakstype.sakskode = sak.sakskode
            LEFT JOIN sakstatus ON sak.sakstatuskode = sakstatus.sakstatuskode
            LEFT JOIN vedtak ON vedtak.sak_id = sak.sak_id
                AND (vedtak.fra_dato <= vedtak.til_dato OR vedtak.til_dato IS NULL)
            WHERE sak.objekt_id = ? AND sak.tabellnavnalias = 'PERS'
            GROUP BY sak.sak_id, sak.aar, sak.lopenrsak, sakstype.sakstypenavn, sak.reg_dato, sak.dato_avsluttet,
                sak.sakstatuskode, sakstatus.sakstatusnavn
        """.trimIndent()

        @Language("OracleSql")
        internal val selectVedtakMedNyesteMaxdato = """
            -- Hent først nyeste vedtak for hver av sakene 
            WITH nyeste_vedtak AS (
                SELECT sak_id, vedtak_id, vedtaktypekode, aktfasekode, unntaksdato, fra_dato FROM (
                    SELECT v.sak_id,
                        v.vedtak_id,
                        v.vedtaktypekode,
                        v.aktfasekode,
                        v.fra_dato,
                        CASE WHEN vf.vedtakverdi IS NOT NULL THEN TO_DATE(vf.vedtakverdi, 'DD-MM-YYYY') END as unntaksdato, -- er bare satt dersom 11-12 unntak er innvilget                        
                        ROW_NUMBER() OVER (PARTITION BY v.sak_id ORDER BY TO_DATE(vf.vedtakverdi, 'DD-MM-YYYY') DESC, v.vedtak_id DESC) as rn
                    FROM vedtak v
                        JOIN VEDTAKFAKTA vf on v.vedtak_id = vf.vedtak_id
                    WHERE v.sak_id in ($SAK_LISTE_TOKEN)
                        AND v.rettighetkode = 'AAP'
                        AND vf.vedtakfaktakode = 'AAPVILKUNN'
                        AND v.vedtaktypekode != 'S' -- stansede vedtak sin maxdato er ikke meningsfull
                ) WHERE rn = 1
            )
            SELECT nv.sak_id, s.reg_dato as sak_registrert_dato, s.dato_avsluttet as sak_avsluttet_dato, s.sakstatuskode as sak_statuskode, 
                nv.vedtak_id, nv.aktfasekode, nv.vedtaktypekode, nv.unntaksdato, nv.fra_dato, 
                vmd.max_unntak_dato
            FROM nyeste_vedtak nv
            JOIN v_vedtak_maxdato vmd ON vmd.vedtak_id = nv.vedtak_id
            JOIN sak s on s.sak_id = nv.sak_id
        """.trimIndent()

    }

}
