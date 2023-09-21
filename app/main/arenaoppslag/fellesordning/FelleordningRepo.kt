package arenaoppslag.fellesordning

import arenaoppslag.dao.VedtakDAO
import java.time.LocalDate
import javax.sql.DataSource

class FelleordningRepo(dataSource: DataSource) {

    private val vedtakDao = VedtakDAO(dataSource)

    fun hentAlleVedtak(fnr: String) = vedtakDao.selectAlleVedtak(fnr)

    fun hentGrunnInfoForAAPMotaker(personId: String, datoForØnsketUttakForAFP:LocalDate):List<FellesordningRespone> = vedtakDao.selectVedtakMedTidsbegrensning(personId, datoForØnsketUttakForAFP)
}