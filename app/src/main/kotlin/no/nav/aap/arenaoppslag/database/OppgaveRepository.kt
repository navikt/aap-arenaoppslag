package no.nav.aap.arenaoppslag.database

import no.nav.aap.arenaoppslag.modeller.ArenaOppgave
import no.nav.aap.arenaoppslag.modeller.PersonId
import org.intellij.lang.annotations.Language
import javax.sql.DataSource

class OppgaveRepository(private val dataSource: DataSource) {

    fun hentOppgaverForPerson(personId: PersonId): List<ArenaOppgave> {
        return dataSource.connection.use { con ->
            con.createParameterizedQuery(selectOppgaverForPerson).use { preparedStatement ->
                preparedStatement.setInt(1, personId.id)
                preparedStatement.executeQuery().map { row ->
                    ArenaOppgave(
                        oppgaveId = row.getLong("task_id"),
                        beskrivelse = row.getString("description"),
                        sakskontekst = row.getString("casecontext"),
                        visningsnavn = row.getString("displayname"),
                        fristDato = DbDato.fraDato(row.getDate("duedate")),
                        arbeidsbenk = row.getString("arbeidsbenk"),
                        oppgaveEnhet = row.getString("oppgave_enhet"),
                        navEnhet = row.getString("nav_enhet"),
                        notat = row.getString("note"),
                    )
                }
            }
        }
    }

    companion object {
        @Language("OracleSql")
        private val selectOppgaverForPerson = """
            SELECT o.task_id,
                   o.description,
                   o.casecontext,
                   o.displayname,
                   o.duedate,
                   o.arbeidsbenk,
                   o.oppgave_enhet,
                   o.nav_enhet,
                   o.note
              FROM v_oppgave o
             WHERE o.person_id = ?
             ORDER BY o.duedate DESC NULLS LAST
        """.trimIndent()
    }
}

