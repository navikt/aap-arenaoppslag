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
import no.nav.aap.arenaoppslag.modeller.Periode
import no.nav.aap.arenaoppslag.modeller.PosteringKilde
import no.nav.aap.arenaoppslag.modeller.ReduksjonRespons
import no.nav.aap.arenaoppslag.modeller.SakId
import no.nav.aap.arenaoppslag.modeller.TilkjentYtelseRad
import no.nav.aap.arenaoppslag.modeller.TilkjentYtelseResponse
import no.nav.aap.arenaoppslag.modeller.tilRespons
import java.time.Duration
import java.time.LocalDate
import java.time.temporal.ChronoUnit
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

        val posteringsrader = meldekortForSak.posteringer.map { postering ->
            val meldekort = postering.meldekortId?.let { meldekortPerId[it] }
            registrerUkjentKilde(postering)

            val timerArbeidetEtterStraff = meldekort?.let { timerArbeidetEtterStraffedager(it) }
            val reduksjon = meldekort?.let {
                byggReduksjon(it, timerArbeidetEtterStraff ?: 0.0, postering)
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

        val meldekortIderMedPostering = meldekortForSak.posteringer.mapNotNull { it.meldekortId }.toSet()
        val meldekortrader = meldekortForSak.meldekort
            .filterNot { it.meldekortId in meldekortIderMedPostering }
            .map { meldekort -> byggRadUtenPostering(meldekort, kvoteSaldo) }

        return TilkjentYtelseResponse(
            sakId = sakId.id,
            rader = (posteringsrader + meldekortrader).sortedWith(radRekkefolge),
        )
    }

    // Meldekort uten postering er levert, men ikke utbetalt (f.eks. full reduksjon eller ikke ferdig
    // beregnet). Dagsatsene ligger på posteringens vedtak, så de er ukjente for disse radene.
    private fun byggRadUtenPostering(meldekort: Meldekort, kvoteSaldo: KvoteSaldo): TilkjentYtelseRad {
        val timerArbeidetEtterStraff = timerArbeidetEtterStraffedager(meldekort)
        return TilkjentYtelseRad(
            fraOgMedDato = meldekort.periode.fraOgMedDato,
            tilOgMedDato = meldekort.periode.tilOgMedDato,
            uke = "${meldekort.ukenrUke1}-${meldekort.ukenrUke2}",
            kilde = PosteringKilde.MELDEKORT,
            dagsatsMedBarnetillegg = null,
            dagsats = null,
            beregnetBrutto = null,
            timerArbeidet = timerArbeidetEtterStraff,
            reduksjon = byggReduksjon(
                meldekort = meldekort,
                timerArbeidet = timerArbeidetEtterStraff,
                postering = null,
            ),
            meldekort = meldekort.tilRespons(),
            gjenstaaendeOrdinaerDager = kvoteSaldo.gjenstaaende(meldekort.meldekortId, KVOTE_ORDINAER),
            gjenstaaendeUnntakDager = kvoteSaldo.gjenstaaende(meldekort.meldekortId, KVOTE_UNNTAK),
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
        // null for meldekort uten postering — da er dagsatser og institusjonsgrad ukjente.
        postering: MeldekortPostering?,
    ): ReduksjonRespons {
        val insGrad = postering?.insGrad
        val dagerForSent = meldekort.reduksjon.dagerForSent
        // Fulltid i en meldekortperiode er 75 timer (10 arbeidsdager à 7,5 t), jf. anmerkningkode TE75T
        // "Arbeidet 75 timer eller mer i perioden sett under ett". Avkortede meldekortperioder har
        // færre dager å fordele timene på, og straffedagene reduserer grunnlaget ytterligere.
        val dagerIPerioden =  antallDagerIPerioden(meldekort.periode) ?: ARBEIDSDAGER_I_MELDEKORTPERIODE
        val aktiveDager = minOf(ARBEIDSDAGER_I_MELDEKORTPERIODE, dagerIPerioden) - dagerForSent

        val timerArbeidetProsent = if (aktiveDager > 0) {
            (timerArbeidet / (aktiveDager * TIMER_PER_DAG) * 100).roundToInt()
        } else {
            0
        }
        val samordningsProsent = beregnSamordningsProsent(postering?.dagsats, postering?.dagsatsForSamordning)
        return ReduksjonRespons(
            levertForSentDager = dagerForSent,
            timerArbeidetProsent = timerArbeidetProsent,
            samordningsProsent = samordningsProsent,
            totalReduksjonProsent = timerArbeidetProsent + samordningsProsent + (insGrad ?: 0),
            fravar = meldekort.reduksjon.fravar,
            sykedager = meldekort.reduksjon.sykedager,
            institusjonsProsent = insGrad,
            anvistProsent = postering?.antall?.let { (it * PROSENT_PER_ANVIST_DAG).roundToInt() },
        )
    }

    private fun beregnSamordningsProsent(dagsats: Int?, dagsatsForSamordning: Int?): Int {
        if (dagsats == null || dagsatsForSamordning == null || dagsatsForSamordning == 0) return 0
        return ((dagsatsForSamordning - dagsats).toDouble() / dagsatsForSamordning * 100).roundToInt()
    }

    // Returnerer null når perioden er ufullstendig, slik at kallstedet kan falle tilbake på
    // normalperioden framfor å regne med et grunnlag vi ikke kjenner.
    private fun antallDagerIPerioden(periode: Periode): Int? {
        val fraOgMed = periode.fraOgMedDato ?: return null
        val tilOgMed = periode.tilOgMedDato ?: return null
        if (tilOgMed.isBefore(fraOgMed)) return 0
        return (ChronoUnit.DAYS.between(fraOgMed, tilOgMed) + 1).toInt()
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
        private const val PROSENT_PER_ANVIST_DAG = 20

        // Rader fra posteringer og rader fra meldekort uten postering slås sammen, og må sorteres
        // kronologisk for at frontend skal vise dem i riktig rekkefølge.
        private val radRekkefolge = compareBy<TilkjentYtelseRad, LocalDate?>(nullsLast()) { it.fraOgMedDato }
            .thenBy(nullsLast<Long>()) { it.meldekort?.meldekortId }
    }
}


