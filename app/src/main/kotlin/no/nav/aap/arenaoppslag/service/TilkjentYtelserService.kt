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
import no.nav.aap.arenaoppslag.modeller.medAnmerkninger
import no.nav.aap.arenaoppslag.modeller.tilRespons
import java.time.DayOfWeek
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
                ?.medAnmerkninger { it.vedtakId == null || it.vedtakId == postering.vedtakId }
            registrerUkjentKilde(postering)


            val timerArbeidetEtterStraff = meldekort?.let {
                timerArbeidetEtterStraffedager(it, snitt(it.periode, postering.periode))
            }
            val reduksjon = meldekort?.let {
                byggReduksjon(it, snitt(it.periode, postering.periode), timerArbeidetEtterStraff ?: 0.0, postering)
            }
            TilkjentYtelseRad(
                posteringId = postering.posteringId,
                posteringTypeKode = postering.posteringTypeKode,
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
                gjenstaaendeOrdinaerDager = postering.meldekortId?.let { kvoteSaldo.gjenstaaende(it, KVOTE_ORDINAER) },
                gjenstaaendeUnntakDager = postering.meldekortId?.let { kvoteSaldo.gjenstaaende(it, KVOTE_UNNTAK) },
                spesialutbetaling = postering.spesialutbetaling?.tilRespons(),
            )
        }


        val posteringsperioderPerMeldekort = meldekortForSak.posteringer
            .mapNotNull { postering -> postering.meldekortId?.let { it to postering.periode } }
            .groupBy({ it.first }, { it.second })
        val meldekortrader = meldekortForSak.meldekort.flatMap { meldekort ->
            udekkedePerioder(meldekort.periode, posteringsperioderPerMeldekort[meldekort.meldekortId].orEmpty())
                .map { delperiode -> byggRadUtenPostering(meldekort, delperiode, kvoteSaldo) }
        }

        return TilkjentYtelseResponse(
            sakId = sakId.id,
            rader = (posteringsrader + meldekortrader).sortedWith(radRekkefolge),
        )
    }


    private fun byggRadUtenPostering(
        meldekort: Meldekort,
        delperiode: Periode,
        kvoteSaldo: KvoteSaldo,
    ): TilkjentYtelseRad {
        val timerArbeidetEtterStraff = timerArbeidetEtterStraffedager(meldekort, delperiode)
        return TilkjentYtelseRad(
            posteringId = null,
            posteringTypeKode = null,
            fraOgMedDato = delperiode.fraOgMedDato,
            tilOgMedDato = delperiode.tilOgMedDato,
            uke = "${meldekort.ukenrUke1}-${meldekort.ukenrUke2}",
            kilde = PosteringKilde.MELDEKORT,
            dagsatsMedBarnetillegg = null,
            dagsats = null,
            beregnetBrutto = null,
            timerArbeidet = timerArbeidetEtterStraff,
            reduksjon = byggReduksjon(
                meldekort = meldekort,
                delperiode = delperiode,
                timerArbeidet = timerArbeidetEtterStraff,
                postering = null,
            ),
            meldekort = meldekort.tilRespons(),
            gjenstaaendeOrdinaerDager = kvoteSaldo.gjenstaaende(meldekort.meldekortId, KVOTE_ORDINAER),
            gjenstaaendeUnntakDager = kvoteSaldo.gjenstaaende(meldekort.meldekortId, KVOTE_UNNTAK),
            spesialutbetaling = null,
        )
    }

    private fun registrerUkjentKilde(postering: MeldekortPostering) {
        if (postering.kilde != PosteringKilde.UKJENT) return
        prometheus.counter(
            "arenaoppslag_postering_ukjent_kilde",
            listOf(Tag.of("alias", postering.kildeAlias ?: "null")),
        ).increment()
    }

    private fun timerArbeidetEtterStraffedager(meldekort: Meldekort, delperiode: Periode): Double {
        val aktivFraOgMed = meldekort.periode.fraOgMedDato?.plusDays(meldekort.reduksjon.dagerForSent.toLong())
        return meldekort.dager
            .filter { aktivFraOgMed == null || !it.dato.isBefore(aktivFraOgMed) }
            .filter { delperiode.inneholder(it.dato) }
            .sumOf { it.timerArbeidet }
    }

    private fun byggReduksjon(
        meldekort: Meldekort,
        delperiode: Periode,
        timerArbeidet: Double,
        postering: MeldekortPostering?,
    ): ReduksjonRespons {
        val insGrad = postering?.insGrad
        val dagerForSent = meldekort.reduksjon.dagerForSent
        val aktiveDager = arbeidsdagerIGrunnlaget(meldekort, delperiode) -
            straffedagerIDelperiode(meldekort, delperiode)

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

    private fun arbeidsdagerIGrunnlaget(meldekort: Meldekort, delperiode: Periode): Int {
        if (delperiode == meldekort.periode) {
            val dagerIPerioden = antallDagerIPerioden(meldekort.periode) ?: ARBEIDSDAGER_I_MELDEKORTPERIODE
            return minOf(ARBEIDSDAGER_I_MELDEKORTPERIODE, dagerIPerioden)
        }
        val fraOgMed = delperiode.fraOgMedDato ?: return ARBEIDSDAGER_I_MELDEKORTPERIODE
        val tilOgMed = delperiode.tilOgMedDato ?: return ARBEIDSDAGER_I_MELDEKORTPERIODE
        return generateSequence(fraOgMed) { it.plusDays(1) }
            .takeWhile { !it.isAfter(tilOgMed) }
            .count { it.dayOfWeek != DayOfWeek.SATURDAY && it.dayOfWeek != DayOfWeek.SUNDAY }
            .coerceAtMost(ARBEIDSDAGER_I_MELDEKORTPERIODE)
    }


    private fun straffedagerIDelperiode(meldekort: Meldekort, delperiode: Periode): Int {
        val dagerForSent = meldekort.reduksjon.dagerForSent
        if (dagerForSent <= 0 || delperiode == meldekort.periode) return dagerForSent
        val start = meldekort.periode.fraOgMedDato ?: return dagerForSent
        val straffeperiode = Periode(start, start.plusDays(dagerForSent - 1L))
        return antallDagerIPerioden(snitt(straffeperiode, delperiode)) ?: 0
    }

    private fun beregnSamordningsProsent(dagsats: Int?, dagsatsForSamordning: Int?): Int {
        if (dagsats == null || dagsatsForSamordning == null || dagsatsForSamordning == 0) return 0
        return ((dagsatsForSamordning - dagsats).toDouble() / dagsatsForSamordning * 100).roundToInt()
    }

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
        private const val KVOTE_ORDINAER = "AAP"
        private const val KVOTE_UNNTAK = "MAAPU"
        private const val ARBEIDSDAGER_I_MELDEKORTPERIODE = 10
        private const val TIMER_PER_DAG = 7.5
        private const val PROSENT_PER_ANVIST_DAG = 20

        private val radRekkefolge = compareBy<TilkjentYtelseRad, LocalDate?>(nullsLast()) { it.fraOgMedDato }
            .thenBy(nullsLast<Long>()) { it.meldekort?.meldekortId }
    }
}


internal fun snitt(a: Periode, b: Periode) = Periode(
    fraOgMedDato = listOfNotNull(a.fraOgMedDato, b.fraOgMedDato).maxOrNull(),
    tilOgMedDato = listOfNotNull(a.tilOgMedDato, b.tilOgMedDato).minOrNull(),
)

internal fun Periode.inneholder(dato: LocalDate): Boolean =
    (fraOgMedDato == null || !dato.isBefore(fraOgMedDato)) &&
        (tilOgMedDato == null || !dato.isAfter(tilOgMedDato))

// Returnerer delene av perioden som ingen av de dekkede periodene overlapper, i kronologisk rekkefølge.
internal fun udekkedePerioder(periode: Periode, dekket: List<Periode>): List<Periode> {
    val fraOgMed = periode.fraOgMedDato ?: return emptyList()
    val tilOgMed = periode.tilOgMedDato ?: return emptyList()
    val resultat = mutableListOf<Periode>()
    var neste = fraOgMed
    dekket
        .map { snitt(it, periode) }
        .filter { !it.tilOgMedDato!!.isBefore(it.fraOgMedDato!!) }
        .sortedBy { it.fraOgMedDato }
        .forEach { del ->
            if (del.fraOgMedDato!!.isAfter(neste)) resultat += Periode(neste, del.fraOgMedDato.minusDays(1))
            if (!del.tilOgMedDato!!.isBefore(neste)) neste = del.tilOgMedDato.plusDays(1)
        }
    if (!neste.isAfter(tilOgMed)) resultat += Periode(neste, tilOgMed)
    return resultat
}


