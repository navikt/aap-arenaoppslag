package arenaoppslag.dsop

import no.nav.aap.arenaoppslag.kontrakt.dsop.Periode
import javax.sql.DataSource

class DsopRepo(private val datasource: DataSource) {
    fun hentMeldeplikt(personId: String, periode: Periode, samtykkePeriode: Periode): MeldekortResponse =
        datasource.connection.use { con ->
            DsopDao.selectMeldekort(personId, periode, samtykkePeriode, con)
        }

    fun hentVedtak(personId: String, periode: Periode, samtykkePeriode: Periode): VedtakResponse =
        datasource.connection.use { con ->
            DsopDao.selectVedtak(personId, periode, samtykkePeriode, con)
        }
}
