package arenaoppslag.dsop

import javax.sql.DataSource

class DsopRepo(datasource: DataSource){
    private val dsopDao = DsopDao(datasource)

    fun hentMeldeplikt(personId: String, periode: Periode, samtykkePeriode: Periode): MeldekortResponse = dsopDao.selectMeldekort(personId, periode, samtykkePeriode)
    fun hentVedtak(personId: String, periode: Periode, samtykkePeriode: Periode): VedtakResponse = dsopDao.selectVedtak(personId, periode, samtykkePeriode)

}