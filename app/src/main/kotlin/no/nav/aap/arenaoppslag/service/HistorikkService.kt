package no.nav.aap.arenaoppslag.service

import com.github.benmanes.caffeine.cache.Caffeine
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics
import no.nav.aap.arenaoppslag.Metrics.prometheus
import no.nav.aap.arenaoppslag.Metrics.registrerAntallSignifikanteVedtak
import no.nav.aap.arenaoppslag.Metrics.registrerSignifikantEnkeltVedtak
import no.nav.aap.arenaoppslag.Metrics.registrerSignifikantVedtak
import no.nav.aap.arenaoppslag.database.HistorikkRepository
import no.nav.aap.arenaoppslag.database.PersonRepository
import no.nav.aap.arenaoppslag.kontrakt.intern.PersonEksistererIAAPArena
import no.nav.aap.arenaoppslag.kontrakt.intern.SignifikanteSakerResponse
import no.nav.aap.arenaoppslag.modeller.ArenaVedtak
import java.time.LocalDate

class HistorikkService(
    private val personRepository: PersonRepository,
    private val historikkRepository: HistorikkRepository
) {

    // Lagrer mappingen fødselsnr -> arena-personId. Bare treff i databasen lagres.
    private val personIdCache = Caffeine.newBuilder()
        .maximumSize(30_000)
        .recordStats()
        .build<String, Int>()

    init {
        CaffeineCacheMetrics.monitor(prometheus, personIdCache, "arenaoppslag_person_id")
    }

    fun signifikanteSakerForPerson(
        fodselsnummerene: Set<String>, virkningstidspunkt: LocalDate
    ): SignifikanteSakerResponse {
        val personId: Int? = hentPersonId(fodselsnummerene)
        if (personId == null) {
            // Personen finnes ikke i AAP-Arena i det hele tatt
            return SignifikanteSakerResponse(harSignifikantHistorikk = false, signifikanteSaker = emptyList())
        }

        val signifikanteVedtak = historikkRepository.hentAlleSignifikanteVedtakForPerson(
            personId,
            virkningstidspunkt
        )
        rapporterMetrikker(signifikanteVedtak)

        val harSignifikantHistorikk = signifikanteVedtak.isNotEmpty()
        val arenaSakIdListe = sorterVedtak(signifikanteVedtak).map { it.sakId }.distinct()

        return SignifikanteSakerResponse(harSignifikantHistorikk, arenaSakIdListe)
    }

    private fun rapporterMetrikker(vedtakene: List<ArenaVedtak>) {
        vedtakene.forEach {
            prometheus.registrerSignifikantVedtak(it)
        }

        if (vedtakene.size == 1) {
            // Bare ett vedtak hindret oss fra å ta inn personen inn i Kelvin
            prometheus.registrerSignifikantEnkeltVedtak(vedtakene.first())
        }

        // Mål antall vedtak som hindret oss fra å ta personen inn i Kelvin, om noen
        prometheus.registrerAntallSignifikanteVedtak(vedtakene.size)
    }

    internal fun sorterVedtak(vedtak: List<ArenaVedtak>): List<ArenaVedtak> {
        // Hvis saker uten tilDato finnes, sorter disse basert på db-order
        val utenSluttdato = vedtak.filter { it.tilDato == null }.reversed() // i reversed db-order (=nyeste først)
        // Hvis saker med tilDato finnes, sorter disse synkende på dato (=nyeste først)
        val medSluttdato = vedtak.filter { it.tilDato != null }.sortedByDescending { it.tilDato }
        return utenSluttdato + medSluttdato
    }

    fun personEksistererIAapArena(fodselsnummerene: Set<String>): PersonEksistererIAAPArena {
        val personId: Int? = hentPersonId(fodselsnummerene)
        return PersonEksistererIAAPArena(personId != null)
    }

    private fun hentPersonId(fodselsnummerene: Set<String>): Int? {
        return fodselsnummerene.firstNotNullOfOrNull { personIdCache.getIfPresent(it) }
            ?: personRepository.hentPersonIdHvisEksisterer(fodselsnummerene)
                ?.also { funnetPersonId ->
                    fodselsnummerene.forEach { personIdCache.put(it, funnetPersonId) }
                }
    }

}