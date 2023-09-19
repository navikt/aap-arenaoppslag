package arenaoppslag

import arenaoppslag.dao.VedtakDao
import arenaoppslag.fellesordning.FellesOrdningDTO
import arenaoppslag.modell.Person
import java.time.LocalDate
import javax.sql.DataSource

class Repo(dataSource: DataSource) {

    private val vedtakDao = VedtakDao(dataSource)

    fun hentAlleVedtak(fnr: String) = vedtakDao.selectAlleVedtak(fnr)

    fun hentGrunnInfoForAAPMotaker(personId: String, datoForØnsketUttakForAFP:LocalDate):List<FellesOrdningDTO> = vedtakDao.selectVedtakMedTidsbegrensning(personId, datoForØnsketUttakForAFP)
}