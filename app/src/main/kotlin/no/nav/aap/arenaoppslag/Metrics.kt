package no.nav.aap.arenaoppslag

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.arenaoppslag.modeller.ArenaVedtak

@Suppress("MagicNumber")
object Metrics {
    val prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    fun MeterRegistry.registrerSignifikantVedtak(vedtak: ArenaVedtak) {
        this.counter("arenaoppslag_signifikant_vedtak", taggListeForVedtak(vedtak))
            .also { counter -> counter.increment() }
    }

    fun MeterRegistry.registrerEnsligSignifikantVedtak(eneste: ArenaVedtak) {
        this.counter("arenaoppslag_eneste_signifikante_vedtak", taggListeForVedtak(eneste))
            .also { counter -> counter.increment() }
    }

    private fun taggListeForVedtak(ettVedtak: ArenaVedtak): List<Tag> = listOf(
        Tag.of("type", ettVedtak.vedtaktypeKode ?: "null"),
        Tag.of("rettighet", ettVedtak.rettighetkode),
        Tag.of("status", ettVedtak.statusKode),
        Tag.of("utfall", ettVedtak.utfallkode ?: "null"),
        Tag.of("aktfase", ettVedtak.aktivitetsfaseKode)
    )

    fun MeterRegistry.registrerNyesteSignifikanteVedtak(siste: ArenaVedtak) {
        this.counter("arenaoppslag_nyeste_signifikante_vedtak", taggListeForVedtak(siste))
            .also { counter -> counter.increment() }
    }

    fun MeterRegistry.registrerAntallSignifikanteVedtak(antall: Int) {
        val bucket = if (antall >= 9) "9+" else antall.toString()
        this.counter("arenaoppslag_signifikante_vedtak_antall", listOf(Tag.of("antall", bucket)))
            .also { counter -> counter.increment() }
    }

}
