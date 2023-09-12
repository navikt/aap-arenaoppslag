package arenaoppslag

import arenaoppslag.dao.VedtakDao
import javax.sql.DataSource

class Repo(dataSource: DataSource) {

    private val vedtakDao = VedtakDao(dataSource)

    fun hentAlleVedtak(fnr: String) = vedtakDao.selectAlleVedtak(fnr)
}