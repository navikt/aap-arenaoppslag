package arenaoppslag.perioder

import java.time.LocalDate
import javax.sql.DataSource

class PerioderRepo(private val dataSource: DataSource) {
    fun hentGrunnInfoForAAPMotaker(personId: String, fraOgMedDato: LocalDate, tilOgMedDato: LocalDate): PerioderResponse =
        dataSource.connection.use { con ->
            PerioderDao.selectVedtakMedTidsbegrensning(personId, fraOgMedDato, tilOgMedDato, con)
        }

    fun hentPeriodeInkludert11_17(personId: String, fraOgMedDato: LocalDate, tilOgMedDato: LocalDate): PerioderMed11_17Response =
        dataSource.connection.use { con ->
            PerioderDao.selectVedtakMedTidsbegrensningOg11_17(personId, fraOgMedDato, tilOgMedDato, con)
        }

    fun hentAktFaseKoder(): List<AktivitetsfaseKode> =
        dataSource.connection.use { con ->
            PerioderDao.selectAktFaseKoder(con)
        }
}
