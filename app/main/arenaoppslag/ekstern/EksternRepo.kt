package arenaoppslag.ekstern

import arenaoppslag.modeller.Maksimum2
import arenaoppslag.modeller.Minimum
import java.time.LocalDate
import javax.sql.DataSource

class EksternRepo(private val dataSource: DataSource) {
    fun hentMinimumLøsning(personId: String, fraOgMedDato: LocalDate, tilOgMedDato: LocalDate): List<Minimum> =
        dataSource.connection.use { con ->
            EksternDao.selectVedtakMinimum(personId, fraOgMedDato, tilOgMedDato, con)
        }

    fun hentMaksimumsløsning(personId: String, fraOgMedDato: LocalDate, tilOgMedDato: LocalDate): Maksimum2 =
        dataSource.connection.use { con ->
            EksternDao.selectVedtakMaksimum(personId, fraOgMedDato, tilOgMedDato, con)
        }
    fun hentFnrForTest(): List<String> =
        dataSource.connection.use { con ->
            EksternDao.selectFnrForTest(con)
        }
}
