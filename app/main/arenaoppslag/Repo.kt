package arenaoppslag

import arenaoppslag.dao.VedtakDao
import java.time.LocalDate
import javax.sql.DataSource

class Repo(dataSource: DataSource) {

    private val vedtakDao = VedtakDao(dataSource)

    fun hentAlleVedtak(fnr: String) = vedtakDao.selectAlleVedtak(fnr)

    fun hentGrunnInfoForAAPMotaker(fnr: String, datoForØnsketUttakForAFP:LocalDate) = vedtakDao.selectVedtakMedTidsbegrensning(fnr, datoForØnsketUttakForAFP)
}