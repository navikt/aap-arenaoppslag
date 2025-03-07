package arenaoppslag.intern

import arenaoppslag.modeller.Maksimum
import no.nav.aap.arenaoppslag.kontrakt.intern.SakStatus
import java.time.LocalDate
import javax.sql.DataSource

class InternRepo(private val dataSource: DataSource) {
    fun hentEksistererIAAPArena(personId: String): Boolean =
        dataSource.connection.use { con ->
            InternDao.selectPersonMedFnrEksisterer(personId, con)
        }

    fun hentMinimumLøsning(
        personId: String,
        fraOgMedDato: LocalDate,
        tilOgMedDato: LocalDate
    ): List<Periode> =
        dataSource.connection.use { con ->
            InternDao.selectVedtakMinimum(personId, fraOgMedDato, tilOgMedDato, con)
        }

    fun hentPeriodeInkludert11_17(
        personId: String,
        fraOgMedDato: LocalDate,
        tilOgMedDato: LocalDate
    ): List<PeriodeMed11_17> =
        dataSource.connection.use { con ->
            InternDao.selectVedtakMedTidsbegrensningOg11_17(
                personId,
                fraOgMedDato,
                tilOgMedDato,
                con
            )
        }

    fun hentMaksimumsløsning(
        personId: String,
        fraOgMedDato: LocalDate,
        tilOgMedDato: LocalDate
    ): Maksimum =
        dataSource.connection.use { con ->
            InternDao.selectVedtakMaksimum(personId, fraOgMedDato, tilOgMedDato, con)
        }

    fun hentSaker(personidentifikator: String): List<SakStatus> {
        return dataSource.connection.use { con ->
            InternDao.selectSaker(personidentifikator, con)
        }
    }
}
