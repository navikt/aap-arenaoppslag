package no.nav.aap.arenaoppslag.database;

import no.nav.aap.arenaoppslag.modeller.PersonId
import org.intellij.lang.annotations.Language
import javax.sql.DataSource;


data class KvoteVerdi(val kode: String, val verdi: Int)

data class KvotebrukHendelse(
    val kvoteBrukId: Int,
    val kvoteTypeKode: String,
    val tabellnavnAliasGrunnlag: String,
    val objektIdGrunnlag: Int,
    val posteringTypeKode: String,
    val antallBevegelse: Int,
    val begrunnelse: String,
    val beregningsleddId: Int,
    val datoFra: String,
    val datoTil: String,
    val verdi: Int,
    val kvoteTypeNavn: String,
    val validerHeleDager: Boolean
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
        SELECT /*+ INDEX(KB KVOTBR_PK) ALL_ROWS */
       KB.KVOTEBRUK_ID,
       KB.KVOTETYPEKODE,
       KB.TABELLNAVNALIAS_GRUNNLAG,
       KB.OBJEKT_ID_GRUNNLAG,
       KB.POSTERINGTYPEKODE,
       KB.ANTALL_BEVEGELSE,
       KB.BEGRUNNELSE,
       BL.BEREGNINGSLEDD_ID,
       BL.DATO_FRA,
       BL.DATO_TIL,
       BL.VERDI,
       KT.KVOTETYPENAVN,
       BLT.VALIDER_HELE_DAGER
FROM BEREGNINGSLEDD BL
JOIN KVOTEBRUK KB ON KB.KVOTEBRUK_ID = BL.OBJEKT_ID_KILDE
JOIN KVOTETYPE KT ON KT.KVOTETYPEKODE = KB.KVOTETYPEKODE
JOIN BEREGNINGSLEDDTYPE BLT ON BL.BEREGNINGSLEDDKODE = BLT.BEREGNINGSLEDDKODE
WHERE BL.TABELLNAVNALIAS_KILDE = 'KVOTBR'
  AND BL.PERSON_ID = ?
        """.trimIndent()

    }

    fun hentKvoteForPerson(personId: PersonId): Set<KvotebrukHendelse> {
        return datasource.connection.use { connection ->
            connection.prepareStatement(selectKvotePåPerson).use { preparedStatement ->
                preparedStatement.setInt(1, personId.id)
                val resultSet = preparedStatement.executeQuery()
                resultSet.map { row ->
                    KvotebrukHendelse(
                        kvoteBrukId = row.getInt("kvotebruk_id"),
                        kvoteTypeKode = row.getString("kvotetypekode"),
                        tabellnavnAliasGrunnlag = row.getString("tabellnavnalias_grunnlag"),
                        objektIdGrunnlag = row.getInt("objekt_id_grunnlag"),
                        posteringTypeKode = row.getString("posteringtypekode"),
                        antallBevegelse = row.getInt("antall_bevegelse"),
                        begrunnelse = row.getString("begrunnelse"),
                        beregningsleddId = row.getInt("beregningsledd_id"),
                        datoFra = row.getString("dato_fra"),
                        datoTil = row.getString("dato_til"),
                        verdi = row.getInt("verdi"),
                        kvoteTypeNavn = row.getString("kvotetypenavn"),
                        validerHeleDager = row.getBoolean("valider_hele_dager")
                    )
                }.toSet()
            }

        }
    }

    fun hentTelleverkForPerson(personId: PersonId): Set<KvoteVerdi> {
        return datasource.connection.use { con ->
            con.prepareStatement(selectTelleverkPåPerson).use { preparedStatement ->
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
