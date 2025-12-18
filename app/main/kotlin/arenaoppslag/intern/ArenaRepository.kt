package arenaoppslag.intern

import arenaoppslag.modeller.Maksimum
import no.nav.aap.arenaoppslag.kontrakt.intern.ArenaSak
import no.nav.aap.arenaoppslag.kontrakt.intern.SakStatus
import java.time.LocalDate
import javax.sql.DataSource

data class KanBehandlesIKelvinDao(val kanBehandles: Boolean, val arenaSakIdListe: List<String>)


class ArenaRepository(private val dataSource: DataSource) {

    fun hentEksistererIAAPArena(fodselsnr: String): Boolean {
        return dataSource.connection.use { con ->
            InternDao.selectPersonMedFnrEksisterer(fodselsnr, con)
        }
    }

    fun hentKanBehandlesIKelvin(personIdentifikatorer: List<String>, søknadMottattPå: LocalDate): KanBehandlesIKelvinDao {
        val relevanteArenaSaker = dataSource.connection.use { con ->
            RelevantHistorikkDao.selectPersonMedRelevantHistorikk(personIdentifikatorer, søknadMottattPå, con)
        }
        val kanBehandles = relevanteArenaSaker.isEmpty()

        val sorterteArenaSaker = sorterSaker(relevanteArenaSaker).map { it.sakId }.distinct()

        return KanBehandlesIKelvinDao(kanBehandles, sorterteArenaSaker)
    }

    internal fun sorterSaker(arenaSaker: List<ArenaSak>): List<ArenaSak> {
        // Hvis saker uten tilOgMedDato finnes, sorter disse basert på db-order
        val sakerUtenSluttDato = arenaSaker.filter { it.tilDato == null }.reversed() // i reversed db-order (nyeste først)
        // Hvis saker uten tilOgMedDato finnes, sorter disse synkende på dato (=nyeste først)
        val sakerMedSluttDato = arenaSaker.filter { it.tilDato != null }.sortedByDescending { it.tilDato }
        return sakerUtenSluttDato + sakerMedSluttDato;
    }

    fun hentPerioder(
        personId: String,
        fraOgMedDato: LocalDate,
        tilOgMedDato: LocalDate
    ): List<Periode> =
        dataSource.connection.use { con ->
            InternDao.selectVedtakPerioder(
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
