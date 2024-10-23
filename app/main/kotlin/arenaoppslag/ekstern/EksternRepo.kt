package arenaoppslag.ekstern

import arenaoppslag.modeller.Maksimum
import java.time.LocalDate
import javax.sql.DataSource

class EksternRepo(private val dataSource: DataSource) {
    fun hentMinimumLøsning(personId: String, fraOgMedDato: LocalDate, tilOgMedDato: LocalDate): VedtakResponse =
        dataSource.connection.use { con ->
            EksternDao.selectVedtakMinimum(personId, fraOgMedDato, tilOgMedDato, con)
        }

    fun hentMaksimumsløsning(personId: String, fraOgMedDato: LocalDate, tilOgMedDato: LocalDate): Maksimum =
        dataSource.connection.use { con ->
            EksternDao.selectVedtakMaksimum(personId, fraOgMedDato, tilOgMedDato, con)
        }
}
