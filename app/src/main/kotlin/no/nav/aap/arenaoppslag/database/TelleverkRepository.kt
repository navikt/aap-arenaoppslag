package no.nav.aap.arenaoppslag.database;

import no.nav.aap.arenaoppslag.modeller.PersonId
import org.intellij.lang.annotations.Language
import java.time.LocalDate
import javax.sql.DataSource;


data class KvoteVerdi(val kode: String, val verdi: Int)

data class KvotebrukHendelse(
    val kvoteBrukId: Int,
    val kvoteTypeKode: String,
    val tabellnavnAliasGrunnlag: String,
    val objektIdGrunnlag: Int,
    val antallBevegelse: Int,
    val posteringTypeKode: String,
    val datoHendelse: LocalDate,
    val modDato: LocalDate?,
    val resterende: Int,
    val modUser: String?,
    val personId: Int,
    val begrunnelse: String?,
)



class TelleverkRepository(private val datasource: DataSource) {

    companion object {
        @Language("OracleSql")
        internal val selectTelleverkPåPerson = """
            SELECT b.verdi, b.beregningsleddkode
            FROM BEREGNINGSLEDD b
            WHERE b.tabellnavnalias_kilde = 'KVOTBR'
              AND b.person_id = ?
              AND b.beregningsleddkode IN ('AAP', 'MAAPU')
        """.trimIndent()

        @Language("OracleSql")
        internal val selectKvotePåPerson = """
            SELECT /*+ INDEX (KVOTEBRUK KVOTBR_PERS_FKI) */
                  KVOTEBRUK_ID
                , KVOTETYPEKODE
                , TABELLNAVNALIAS_GRUNNLAG
                , OBJEKT_ID_GRUNNLAG
                , ANTALL_BEVEGELSE
                , POSTERINGTYPEKODE
                , DATO_HENDELSE
                , MOD_DATO
                ,(select nvl(sum(k2.antall_bevegelse), 0)
                  from kvotebruk k2
                  where k2.person_id     = KV.Person_Id
                  and k2.kvotetypekode = kv.KvotetypeKode
                  and k2.kvotebruk_id <= KV.Kvotebruk_id
                  and k2.kvotebruk_id >= ( select max(k3.kvotebruk_id)
                                           from kvotebruk k3
                                           where k3.person_id     = KV.Person_Id
                                           and k3.kvotetypekode = kv.KvotetypeKode
                                           and k3.kvotebruk_id <= kv.Kvotebruk_id
                                           and k3.posteringtypekode in ('INIT','NULLE'))) resterende
                , MOD_USER
                , PERSON_ID
                , BEGRUNNELSE
            FROM KVOTEBRUK kv
            WHERE person_id = ?
              AND kv.tabellnavnalias_grunnlag = 'MKORT'
              AND kv.kvotetypekode IN ('AAP', 'MAAPU')
              AND kv.posteringtypekode = 'OPPD'
            ORDER BY kv.dato_hendelse
        """.trimIndent()

    }

    fun hentKvoteForPerson(personId: PersonId): Set<KvotebrukHendelse> {
        return datasource.connection.use { connection ->
            connection.createParameterizedQuery(selectKvotePåPerson).use { preparedStatement ->
                preparedStatement.setInt(1, personId.id)
                val resultSet = preparedStatement.executeQuery()
                resultSet.map { row ->
                    KvotebrukHendelse(
                        kvoteBrukId = row.getInt("kvotebruk_id"),
                        kvoteTypeKode = row.getString("kvotetypekode"),
                        tabellnavnAliasGrunnlag = row.getString("tabellnavnalias_grunnlag"),
                        objektIdGrunnlag = row.getInt("objekt_id_grunnlag"),
                        antallBevegelse = row.getInt("antall_bevegelse"),
                        posteringTypeKode = row.getString("posteringtypekode"),
                        datoHendelse = row.getDate("dato_hendelse").toLocalDate(),
                        modDato = row.getDate("mod_dato")?.toLocalDate(),
                        resterende = row.getInt("resterende"),
                        modUser = row.getString("mod_user"),
                        personId = row.getInt("person_id"),
                        begrunnelse = row.getString("begrunnelse"),
                    )
                }.toSet()
            }
        }
    }

    fun hentTelleverkForPerson(personId: PersonId): Set<KvoteVerdi> {
        return datasource.connection.use { con ->
            con.createParameterizedQuery(selectTelleverkPåPerson).use { preparedStatement ->
                preparedStatement.setInt(1, personId.id)
                val resultSet = preparedStatement.executeQuery()
                resultSet.map { row ->
                    KvoteVerdi(
                        kode = row.getString("beregningsleddkode"),
                        verdi = row.getInt("verdi")
                    )
                }.toSet()
            }
        }
    }
}
