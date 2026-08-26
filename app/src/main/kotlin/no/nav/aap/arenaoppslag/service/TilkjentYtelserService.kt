package no.nav.aap.arenaoppslag.service

import com.github.benmanes.caffeine.cache.Caffeine
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics
import no.nav.aap.arenaoppslag.Metrics.prometheus
import no.nav.aap.arenaoppslag.database.MeldekortRepository
import no.nav.aap.arenaoppslag.modeller.KvotebrukHendelse
import no.nav.aap.arenaoppslag.modeller.Meldekort
import no.nav.aap.arenaoppslag.modeller.MeldekortPostering
import no.nav.aap.arenaoppslag.modeller.PersonId
import no.nav.aap.arenaoppslag.modeller.PosteringKilde
import no.nav.aap.arenaoppslag.modeller.ReduksjonRespons
import no.nav.aap.arenaoppslag.modeller.SakId
import no.nav.aap.arenaoppslag.modeller.TilkjentYtelseRad
import no.nav.aap.arenaoppslag.modeller.TilkjentYtelseResponse
import no.nav.aap.arenaoppslag.modeller.tilRespons
import java.time.Duration
import kotlin.math.roundToInt


@Suppress("MagicNumber")
class TilkjentYtelserService(
    private val meldekortRepository: MeldekortRepository,
    private val telleverkService: TelleverkService,
) {

    private val tilkjentYtelseCache = Caffeine.newBuilder()
        .maximumSize(10_000)
        .expireAfterWrite(Duration.ofMinutes(15))
        .build<Int, TilkjentYtelseResponse>()

    init {
        CaffeineCacheMetrics.monitor(prometheus, tilkjentYtelseCache, "arenaoppslag_tilkjent_ytelse_per_sak")
    }

    fun hentTilkjenteYtelserForSak(sakId: SakId): TilkjentYtelseResponse =
        tilkjentYtelseCache.get(sakId.id) { byggTilkjenteYtelserForSak(sakId) }

    private fun byggTilkjenteYtelserForSak(sakId: SakId): TilkjentYtelseResponse {
        val meldekortForSak = meldekortRepository.hentForSak(sakId)
        val meldekortPerId = meldekortForSak.meldekort.associateBy { it.meldekortId }

        val personId = meldekortForSak.posteringer.firstOrNull()?.personId ?: meldekortForSak.meldekort.firstOrNull()?.personId
        val kvoteSaldo = KvoteSaldo(
            personId?.let { telleverkService.hentKvoteBrukHendelserForPerson(PersonId(it)) }.orEmpty()
        )

        val rader = meldekortForSak.posteringer.map { postering ->
            val meldekort = postering.meldekortId?.let { meldekortPerId[it] }
            registrerUkjentKilde(postering)

            val timerArbeidetEtterStraff = meldekort?.let { timerArbeidetEtterStraffedager(it) }
            val reduksjon = meldekort?.let {
                byggReduksjon(it, timerArbeidetEtterStraff ?: 0.0, postering.dagsats, postering.dagsatsForSamordning, postering.insGrad)
            }
            TilkjentYtelseRad(
                fraOgMedDato = postering.periode.fraOgMedDato,
                tilOgMedDato = postering.periode.tilOgMedDato,
                uke = meldekort?.let { "${it.ukenrUke1}-${it.ukenrUke2}" },
                kilde = postering.kilde,
                dagsatsMedBarnetillegg = postering.dagsatsMedBarnetillegg,
                dagsats = postering.dagsats,
                beregnetBrutto = postering.belop,
                timerArbeidet = timerArbeidetEtterStraff,
                reduksjon = reduksjon,
                meldekort = meldekort?.tilRespons(),
                // Kvotetrekk registreres kun per meldekort, så spesialutbetalinger får ingen saldo.
                gjenstaaendeOrdinaerDager = postering.meldekortId?.let { kvoteSaldo.gjenstaaende(it, KVOTE_ORDINAER) },
                gjenstaaendeUnntakDager = postering.meldekortId?.let { kvoteSaldo.gjenstaaende(it, KVOTE_UNNTAK) },
            )
        }

        return TilkjentYtelseResponse(
            sakId = sakId.id,
            rader = rader,
        )
    }


    // Ukjente kildealiaser telles slik at vi oppdager nye verdier i TABELLNAVNALIAS_KILDE
    // uten å måtte lete i loggene. Aliaset er en kodetabellverdi, så kardinaliteten er lav.
    private fun registrerUkjentKilde(postering: MeldekortPostering) {
        if (postering.kilde != PosteringKilde.UKJENT) return
        prometheus.counter(
            "arenaoppslag_postering_ukjent_kilde",
            listOf(Tag.of("alias", postering.kildeAlias ?: "null")),
        ).increment()
    }

    private fun timerArbeidetEtterStraffedager(meldekort: Meldekort): Double {
        val aktivFraOgMed = meldekort.periode.fraOgMedDato?.plusDays(meldekort.reduksjon.dagerForSent.toLong())
        return meldekort.dager
            .filter { !it.dato.isBefore(aktivFraOgMed) }
            .sumOf { it.timerArbeidet }
    }

    private fun byggReduksjon(
        meldekort: Meldekort,
        timerArbeidet: Double,
        dagsats: Int?,
        dagsatsForSamordning: Int?,
        insGrad: Int?,
    ): ReduksjonRespons {
        val dagerForSent = meldekort.reduksjon.dagerForSent
        // Fulltid i en meldekortperiode er 75 timer (10 arbeidsdager à 7,5 t), jf. anmerkningkode TE75T
        // "Arbeidet 75 timer eller mer i perioden sett under ett". Straffedagene reduserer antall
        // dager som inngår i fulltidsgrunnlaget.
        val aktiveDager = ARBEIDSDAGER_I_MELDEKORTPERIODE - dagerForSent
        val timerArbeidetProsent = if (aktiveDager > 0) {
            (timerArbeidet / (aktiveDager * TIMER_PER_DAG) * 100).roundToInt()
        } else {
            0
        }
        val samordningsProsent = beregnSamordningsProsent(dagsats, dagsatsForSamordning)
        return ReduksjonRespons(
            levertForSentDager = dagerForSent,
            timerArbeidetProsent = timerArbeidetProsent,
            samordningsProsent = samordningsProsent,
            totalReduksjonProsent = timerArbeidetProsent + samordningsProsent + (insGrad ?: 0),
            fravar = meldekort.reduksjon.fravar,
            sykedager = meldekort.reduksjon.sykedager,
            institusjonsProsent = insGrad,
        )
    }


    private fun beregnSamordningsProsent(dagsats: Int?, dagsatsForSamordning: Int?): Int {
        if (dagsats == null || dagsatsForSamordning == null || dagsatsForSamordning == 0) return 0
        return ((dagsatsForSamordning - dagsats).toDouble() / dagsatsForSamordning * 100).roundToInt()
    }

    /**
     * Slår opp gjenstående kvotesaldo på det tidspunktet et gitt meldekort ble beregnet.
     * KVOTEBRUK er en løpende hovedbok per person, der `resterende` allerede er akkumulert
     * saldo til og med hver enkelt bevegelse. Rekkefølgen følger kvotebruk_id, ikke dato_hendelse,
     * fordi det er den samme rekkefølgen den akkumulerte summen beregnes med.
     */
    private class KvoteSaldo(hendelser: Collection<KvotebrukHendelse>) {
        private val hendelserSortert = hendelser.sortedBy { it.id }

        private val sisteHendelseIdPerMeldekort: Map<Long, Int> = hendelserSortert
            .filter { it.endringsGrunnlag == GRUNNLAG_MELDEKORT }
            .groupBy { it.objektIdGrunnlag }
            .mapValues { (_, hendelserForMeldekort) -> hendelserForMeldekort.maxOf { it.id } }

        fun gjenstaaende(meldekortId: Long, kvoteTypeKode: String): Int? {
            val sisteHendelseId = sisteHendelseIdPerMeldekort[meldekortId] ?: return null
            return hendelserSortert
                .lastOrNull { it.kvoteTypeKode == kvoteTypeKode && it.id <= sisteHendelseId }
                ?.resterende
        }

        private companion object {
            private const val GRUNNLAG_MELDEKORT = "MKORT"
        }
    }

    private companion object {
        // Kvotekoder: AAP = ordinær periode, MAAPU = unntak §11-12.
        private const val KVOTE_ORDINAER = "AAP"
        private const val KVOTE_UNNTAK = "MAAPU"
        private const val ARBEIDSDAGER_I_MELDEKORTPERIODE = 10
        private const val TIMER_PER_DAG = 7.5
    }
}


