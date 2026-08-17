package no.nav.aap.arenaoppslag.service

import com.github.benmanes.caffeine.cache.Caffeine
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics
import no.nav.aap.arenaoppslag.Metrics.prometheus
import no.nav.aap.arenaoppslag.Metrics.registrerNyesteSignifikanteVedtakMedAntall
import no.nav.aap.arenaoppslag.Metrics.registrerSignifikantVedtak
import no.nav.aap.arenaoppslag.database.HistorikkRepository
import no.nav.aap.arenaoppslag.database.PersonRepository
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SignifikantHistorikkResponse
import no.nav.aap.arenaoppslag.kontrakt.intern.PersonEksistererIAAPArena
import no.nav.aap.arenaoppslag.modeller.ArenaVedtak
import no.nav.aap.arenaoppslag.modeller.PersonId
import java.time.LocalDate

class HistorikkService(
    private val personRepository: PersonRepository,
    private val historikkRepository: HistorikkRepository,
) {

    // Lagrer mappingen fødselsnr -> arena-personId. Bare treff i databasen lagres.
    @Suppress("MagicNumber")
    private val personIdCache = Caffeine.newBuilder()
        .maximumSize(30_000)
        .recordStats()
        .build<String, Int>()

    init {
        CaffeineCacheMetrics.monitor(prometheus, personIdCache, "arenaoppslag_person_id")
    }

    fun signifikantHistorikk(
        personId: PersonId,
        virkningstidspunkt: LocalDate
    ): SignifikantHistorikkResponse {
        val signifikanteVedtak = historikkRepository.hentAlleSignifikanteVedtakForPerson(
            personId,
            virkningstidspunkt
        )
        rapporterMetrikker(signifikanteVedtak)

        val harSignifikantHistorikk = signifikanteVedtak.isNotEmpty()
        val arenaSakIdListe = sorterVedtak(signifikanteVedtak)

        return SignifikantHistorikkResponse(harSignifikantHistorikk, arenaSakIdListe.map {
            it.tilKontrakt()
        })
    }

    private fun rapporterMetrikker(vedtakene: List<ArenaVedtak>) {
        if (vedtakene.isEmpty()) return

        vedtakene.forEach {
            prometheus.registrerSignifikantVedtak(it)
        }

        prometheus.registrerNyesteSignifikanteVedtakMedAntall(vedtakene.first(), vedtakene.size)
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
        // TODO deprekert, fjern når kallere er oppdatert
        return fodselsnummerene.firstNotNullOfOrNull { personIdCache.getIfPresent(it) }
            ?: personRepository.hentPersonIdHvisEksisterer(fodselsnummerene)
                ?.also { funnetPersonId ->
                    fodselsnummerene.forEach { personIdCache.put(it, funnetPersonId.id) }
                }?.id
    }

}
