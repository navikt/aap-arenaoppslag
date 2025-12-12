package arenaoppslag.intern

import arenaoppslag.modeller.Maksimum
import no.nav.aap.arenaoppslag.kontrakt.intern.ArenaSak
import no.nav.aap.arenaoppslag.kontrakt.intern.SakStatus
import java.time.LocalDate
import javax.sql.DataSource

data class KanBehandlesIKelvinDao(val kanBehandles: Boolean, val personIdentifikator: String, val sakId: String?)


class ArenaRepository(private val dataSource: DataSource) {

    fun hentEksistererIAAPArena(fodselsnr: String): Boolean {
        return dataSource.connection.use { con ->
            InternDao.selectPersonMedFnrEksisterer(fodselsnr, con)
        }
    }

    fun hentKanBehandlesIKelvin(personId: String, søknadMottattPå: LocalDate): KanBehandlesIKelvinDao {
        val relevanteArenaSaker = dataSource.connection.use { con ->
            RelevantHistorikkDao.selectPersonMedRelevantHistorikk(personId, søknadMottattPå, con)
        }
        val kanBehandles = relevanteArenaSaker.isEmpty()

        val nyesteSak = finnNyesteSakId(relevanteArenaSaker)

        return KanBehandlesIKelvinDao(kanBehandles, personId, nyesteSak)
    }

    internal fun finnNyesteSakId(arenaSaker: List<ArenaSak>): String? {
        val sakerMedSluttDato = arenaSaker.filter { it.tilDato != null }
        // Hvis saker uten tilOgMedDato finnes, ta den nyeste av disse basert på db-order:
        val nyesteSak = arenaSaker.findLast { it.tilDato == null }?.sakId
        // ellers ta den nyeste saken basert på tilOgMedDato
            ?: sakerMedSluttDato.sortedBy { it.tilDato }.lastOrNull()?.sakId
        return nyesteSak
    }

    private object RateBegrenser {
        private val INNSLIPP_PROSENT = 20

        fun personenTasMed(personId: String): Boolean {
            return INNSLIPP_PROSENT >= (personId.hashCode() % 100 + 1)
        }
    }

    fun rateBegrensetHentKanBehandlesIKelvin(personId: String, søknadMottattPå: LocalDate): KanBehandlesIKelvinDao {
        // Vurder etter nye regler om personen kan behandles i Kelvin
        val personenKanBehandlesIKelvin = hentKanBehandlesIKelvin(personId, søknadMottattPå)
        // Midlertidig: Begrens hvor mange personer vi tar inn i Kelvin
        return if (personenKanBehandlesIKelvin.kanBehandles && RateBegrenser.personenTasMed(personId)) {
            personenKanBehandlesIKelvin
        } else {
            // Negativt svar for å begrense antall personer som tas inn i Kelvin
            KanBehandlesIKelvinDao(false, personId, null)
        }

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
