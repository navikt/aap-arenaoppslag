package arenaoppslag.intern

import arenaoppslag.modeller.Maksimum
import no.nav.aap.arenaoppslag.kontrakt.intern.ArenaSak
import no.nav.aap.arenaoppslag.kontrakt.intern.SakStatus
import java.time.LocalDate
import javax.sql.DataSource


class ArenaRepository(private val dataSource: DataSource) {

    fun hentEksistererIAAPArena(fodselsnr: String): Boolean {
        return dataSource.connection.use { con ->
            RelevantHistorikkDao.selectPersonMedFnrEksisterer(fodselsnr, con)
        }
    }

    fun hentKanBehandlesIKelvin(personIdentifikatorer: List<String>, søknadMottattPå: LocalDate):
            KanBehandlesIKelvinDao {
        val relevanteArenaSaker = dataSource.connection.use { con ->
            RelevantHistorikkDao.selectPersonMedRelevantHistorikk(
                personIdentifikatorer, søknadMottattPå, con
            )
        }
        val kanBehandles = relevanteArenaSaker.isEmpty()

        val sorterteArenaSaker = sorterSaker(relevanteArenaSaker).map { it.sakId }.distinct()

        return KanBehandlesIKelvinDao(kanBehandles, sorterteArenaSaker)
    }

    internal fun sorterSaker(arenaSaker: List<ArenaSak>): List<ArenaSak> {
        // Hvis saker uten tilDato finnes, sorter disse basert på db-order
        val utenSluttdato = arenaSaker.filter { it.tilDato == null }.reversed() // i reversed db-order (=nyeste først)
        // Hvis saker med tilDato finnes, sorter disse synkende på dato (=nyeste først)
        val medSluttdato = arenaSaker.filter { it.tilDato != null }.sortedByDescending { it.tilDato }
        return utenSluttdato + medSluttdato
    }

    fun hentPerioder(
        personId: String,
        fraOgMedDato: LocalDate,
        tilOgMedDato: LocalDate
    ): List<Periode> =
        dataSource.connection.use { con ->
            PeriodeDao.selectVedtakPerioder(
                fodselsnr = personId,
                fraOgMedDato = fraOgMedDato,
                tilOgMedDato = tilOgMedDato,
                connection = con
            )
        }

    fun hentPeriodeInkludert11_17(
        personId: String,
        fraOgMedDato: LocalDate,
        tilOgMedDato: LocalDate
    ): List<PeriodeMed11_17> =
        dataSource.connection.use { con ->
            PeriodeDao.selectVedtakMedTidsbegrensningOg11_17(
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
            MaksimumDao.selectVedtakMaksimum(personId, fraOgMedDato, tilOgMedDato, con)
        }

    fun hentSaker(personidentifikator: String): List<SakStatus> {
        return dataSource.connection.use { con ->
            SakerDao.selectSaker(personidentifikator, con)
        }
    }
}
