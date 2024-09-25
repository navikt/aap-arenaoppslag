package arenaoppslag.fellesordningen

import java.time.LocalDate
import javax.sql.DataSource

class FellesordningenRepo(private val dataSource: DataSource) {
    fun hentGrunnInfoForAAPMotaker(personId: String, fraOgMedDato: LocalDate, tilOgMedDato: LocalDate): VedtakResponse =
        dataSource.connection.use { con ->
            FellesordningenDao.selectVedtakMedTidsbegrensning(personId, fraOgMedDato, tilOgMedDato, con)
        }
}
