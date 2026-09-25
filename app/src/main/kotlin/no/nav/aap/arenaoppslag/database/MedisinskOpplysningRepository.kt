package no.nav.aap.arenaoppslag.database

import no.nav.aap.arenaoppslag.database.DbDato.fraDato
import no.nav.aap.arenaoppslag.modeller.MedisinskOpplysning
import no.nav.aap.arenaoppslag.modeller.PersonId
import org.intellij.lang.annotations.Language
import java.sql.ResultSet
import javax.sql.DataSource

class MedisinskOpplysningRepository(private val dataSource: DataSource) {

    fun hentForPerson(personId: PersonId): List<MedisinskOpplysning> =
        dataSource.connection.use { con ->
            con.createParameterizedQuery(selectMedisinskeOpplysningerForPerson).use { preparedStatement ->
                preparedStatement.setInt(1, personId.id)
                preparedStatement.executeQuery()
                    .map { row -> mapperForMedisinskOpplysning(row) }
            }
        }

    companion object {
        // Kolonnen heter ATTFORINGSOPPLYSNING_ID (med ekstra S) i MEDISINSK_OPPLYSNING i Arena
        @Language("OracleSql")
        private val selectMedisinskeOpplysningerForPerson = """
            SELECT mo.medisinsk_opplysning_id, mo.diagnoseklassekode, mo.diagnosekode,
                   mo.diagnosetypekode, mo.kilde_dato
              FROM attforingopplysning ao
              JOIN medisinsk_opplysning mo ON mo.attforingsopplysning_id = ao.attforingopplysning_id
             WHERE ao.person_id = ?
             ORDER BY mo.kilde_dato, mo.medisinsk_opplysning_id
        """.trimIndent()

        private fun mapperForMedisinskOpplysning(row: ResultSet) = MedisinskOpplysning(
            medisinskOpplysningId = row.getLong("medisinsk_opplysning_id"),
            diagnoseklassekode = row.getString("diagnoseklassekode"),
            diagnosekode = row.getString("diagnosekode"),
            diagnosetypekode = row.getString("diagnosetypekode"),
            kildeDato = requireNotNull(fraDato(row.getDate("kilde_dato"))),
        )
    }
}
