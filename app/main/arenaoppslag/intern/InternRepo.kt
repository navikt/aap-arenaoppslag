package arenaoppslag.intern

import arenaoppslag.modeller.Maksimum
import java.time.LocalDate
import javax.sql.DataSource

class InternRepo(private val dataSource: DataSource) {
    fun hentMinimumLøsning(personId: String, fraOgMedDato: LocalDate, tilOgMedDato: LocalDate): VedtakResponse =
        dataSource.connection.use { con ->
            InternDao.selectVedtakMinimum(personId, fraOgMedDato, tilOgMedDato, con)
        }

    fun hentMaksimumsløsning(personId: String, fraOgMedDato: LocalDate, tilOgMedDato: LocalDate): Maksimum =
        dataSource.connection.use { con ->
            InternDao.selectVedtakMaksimum(personId, fraOgMedDato, tilOgMedDato, con)
        }

    fun hentSaker(personidentifikator: String): List<SakStatus> {
        return dataSource.connection.use { con ->
            InternDao.selectSaker(personidentifikator, con)
        }
    }
}
