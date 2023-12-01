package arenaoppslag.fellesordningen

import java.time.LocalDate
import javax.sql.DataSource

class FellesordningenRepo(dataSource: DataSource) {

    private val fellesordningenDao = FellesordningenDao(dataSource)

    fun hentGrunnInfoForAAPMotaker(personId: String, fraOgMedDato: LocalDate, tilOgMedDato: LocalDate): VedtakResponse =
        fellesordningenDao.selectVedtakMedTidsbegrensning(personId, fraOgMedDato, tilOgMedDato)

}
