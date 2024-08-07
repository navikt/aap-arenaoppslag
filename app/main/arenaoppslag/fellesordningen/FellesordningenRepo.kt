package arenaoppslag.fellesordningen

import arenaoppslag.modeller.Maksimum2
import java.time.LocalDate
import javax.sql.DataSource

class FellesordningenRepo(private val dataSource: DataSource) {
    fun hentGrunnInfoForAAPMotaker(personId: String, fraOgMedDato: LocalDate, tilOgMedDato: LocalDate): VedtakResponse =
        dataSource.connection.use { con ->
            FellesordningenDao.selectVedtakMedTidsbegrensning(personId, fraOgMedDato, tilOgMedDato, con)
        }

    fun selectMaksimumsløsning(personId: String, fraOgMedDato: LocalDate, tilOgMedDato: LocalDate): Maksimum2 =
        dataSource.connection.use { con ->
            FellesordningenDao.selectVedtakMaksimum(personId, fraOgMedDato, tilOgMedDato, con)
        }
}
