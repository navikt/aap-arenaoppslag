package arenaoppslag.intern

import arenaoppslag.Metrics.prometheus
import arenaoppslag.modeller.Maksimum
import no.nav.aap.arenaoppslag.kontrakt.intern.ArenaSak
import no.nav.aap.arenaoppslag.kontrakt.intern.SakStatus
import org.slf4j.LoggerFactory
import java.time.LocalDate
import javax.sql.DataSource

data class KanBehandlesIKelvinDao(val kanBehandles: Boolean, val personIdentifikator: String, val sakId: String?)


class ArenaRepository(private val dataSource: DataSource) {
    companion object {
        private val logger = LoggerFactory.getLogger(ArenaRepository::class.java)
    }

    fun hentEksistererIAAPArena(personId: String): Boolean {
        val finnesIArenaEtterGammelRegel = dataSource.connection.use { con ->
            InternDao.selectPersonMedFnrEksisterer(personId, con)
        }

        runCatching {
            // Sjekk med vår nye logikk om personen kan behandles i Kelvin
            val nyRegel = hentKanBehandlesIKelvin(personId)
            if (finnesIArenaEtterGammelRegel) {
                prometheus.counter("arenaoppslag_gammel_regel_match").increment()
            }
            val finnesIArenaEtterNyRegel = !nyRegel.kanBehandles
            if (finnesIArenaEtterNyRegel) {
                prometheus.counter("arenaoppslag_ny_regel_1_match").increment()
            }

            val godkjentKunAvNyeRegler = finnesIArenaEtterGammelRegel && nyRegel.kanBehandles
            if (godkjentKunAvNyeRegler) {
                logger.info("Person avvist etter gammel regel ble tatt inn av ny regel, sakId=${nyRegel.sakId}")
                prometheus.counter("arenaoppslag_inntak_etter_ny_regel").increment()
            }
        }.onFailure {
            logger.warn("Feil i ny spørring på arena-historikk", it)
        }

        return finnesIArenaEtterGammelRegel
    }

    fun hentKanBehandlesIKelvin(personId: String): KanBehandlesIKelvinDao {
        val relevanteArenaSaker = dataSource.connection.use { con ->
            InternDao.selectPersonMedRelevanteRettighetskoder(personId, con)
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
        val personenKanBehandlesIKelvin = hentKanBehandlesIKelvin(personId)
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
                personId = personId,
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
