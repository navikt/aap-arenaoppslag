package no.nav.aap.arenaoppslag.database

import no.nav.aap.arenaoppslag.kontrakt.intern.Person
import org.jetbrains.annotations.TestOnly
import java.sql.Connection
import javax.sql.DataSource


class PersonRepository(private val dataSource: DataSource) {

    fun hentPersonIdHvisEksisterer(fodselnummerene: Set<String>): Int? {
        dataSource.connection.use { con ->
            return selectPersonIdFraFnr(fodselnummerene, con)
        }
    }

    @TestOnly
    fun hentAlle(): List<Person> {
        return dataSource.connection.use { con ->
            hentAllePersoner(con)
        }
    }

    companion object {

        private const val FNR_LISTE_TOKEN = "?:fodselsnummer"
        private fun queryMedFodselsnummerListe(baseQuery: String, fodselsnummerene: Set<String>): String {
            // Oracle lar oss ikke bruke liste-parameter i prepared statements, så vi bygger inn fødselsnumrene direkte
            // i spørringen her
            val allePersonensFodselsnummer = fodselsnummerene.joinToString(separator = ",") { "'$it'" }
            return baseQuery.replace(FNR_LISTE_TOKEN, allePersonensFodselsnummer)
        }

        fun selectPersonIdFraFnr(fodselsnr: Set<String>, connection: Connection): Int? {
            val baseQuery = """
                select person_id from person
                where fodselsnr in ($FNR_LISTE_TOKEN)
                """
            val query = queryMedFodselsnummerListe(baseQuery, fodselsnr)

            val liste = connection.prepareStatement(query).use { preparedStatement ->
                val resultSet = preparedStatement.executeQuery()
                resultSet.map { row -> row.getInt("person_id") }
            }

            require(liste.size <= 1) { "Forventet maks en person_id for fnr-liste" }

            return liste.firstOrNull()
        }

        fun hentAllePersoner(connection: Connection): List<Person> {
            val query = "SELECT fodselsnr, fornavn, etternavn FROM person"
            connection.prepareStatement(query).use { preparedStatement ->
                val resultSet = preparedStatement.executeQuery()
                return resultSet.map { row ->
                    Person(
                        personIdentifikator = row.getString("fodselsnr"),
                        fornavn = row.getString("fornavn"),
                        etternavn = row.getString("etternavn")
                    )
                }
            }
        }
    }
}
