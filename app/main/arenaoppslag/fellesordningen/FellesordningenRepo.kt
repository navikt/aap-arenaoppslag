package arenaoppslag.fellesordningen

import java.time.LocalDate
import javax.sql.DataSource

class FellesordningenRepo(dataSource: DataSource) {

    private val fellesordningenDao = FellesordningenDao(dataSource)

    fun hentGrunnInfoForAAPMotaker(personId: String, datoForØnsketUttakForAFP: LocalDate): VedtakResponse =
        fellesordningenDao.selectVedtakMedTidsbegrensning(personId, datoForØnsketUttakForAFP)

}
