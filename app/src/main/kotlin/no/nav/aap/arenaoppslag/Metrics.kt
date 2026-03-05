package no.nav.aap.arenaoppslag

import io.micrometer.core.instrument.DistributionSummary
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.arenaoppslag.modeller.ArenaVedtak

object Metrics {
    val prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    fun MeterRegistry.registrerSignifikantVedtak(vedtak: ArenaVedtak) {
        this.counter(
            "arenaoppslag_signifikant_vedtak",
            taggListeForVedtak(vedtak)
        ).also { counter -> counter.increment() }
    }

    fun PrometheusMeterRegistry.registrerSignifikantEnkeltVedtak(ettVedtak: ArenaVedtak) {
        this.counter(
            "arenaoppslag_signifikant_enkeltvedtak",
            taggListeForVedtak(ettVedtak)
        ).also { counter -> counter.increment() }
    }

    private fun taggListeForVedtak(ettVedtak: ArenaVedtak): List<Tag> = listOf(
        Tag.of("type", ettVedtak.vedtaktypeKode ?: "null"),
        Tag.of("rettighet", ettVedtak.rettighetkode),
        Tag.of("status", ettVedtak.statusKode),
        Tag.of("utfall", ettVedtak.utfallkode ?: "null")
    )

    fun MeterRegistry.registrerAntallSignifikanteVedtak(size: Int) {
        val slo = listOf(1.0, 2.0, 3.0, 4.0, 5.0, 10.0, 15.0, 20.0, 25.0).toDoubleArray()
        DistributionSummary.builder("arenaoppslag_antall_signifikante_vedtak")
            .serviceLevelObjectives(*slo)
            .register(this)
            .record(size.toDouble())
    }
}
