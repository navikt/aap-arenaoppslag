package arenaoppslag.perioder

import java.time.LocalDate
import javax.sql.DataSource

class PerioderRepo(private val dataSource: DataSource) {
    fun hentGrunnInfoForAAPMotaker(personId: String, fraOgMedDato: LocalDate, tilOgMedDato: LocalDate): PerioderResponse =
        dataSource.connection.use { con ->
            PerioderDao.selectVedtakMedTidsbegrensning(personId, fraOgMedDato, tilOgMedDato, con)
        }
}
