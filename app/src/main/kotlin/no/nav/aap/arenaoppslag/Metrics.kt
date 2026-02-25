package no.nav.aap.arenaoppslag

import io.micrometer.core.instrument.DistributionSummary
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.arenaoppslag.modeller.ArenaVedtak
import java.util.stream.IntStream.range
import kotlin.streams.toList

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

    private val verdierForHistogram = range(1, 25).toList().map { it.toDouble() }.toDoubleArray()
    private val antallVedtakHistogram = DistributionSummary.builder("arenaoppslag_signifikante_vedtak_antall")
        .maximumExpectedValue(25.0)
        .baseUnit("antall")
        .scale(1.0)
        .serviceLevelObjectives(*verdierForHistogram)

    fun MeterRegistry.registrerAntallSignifikanteVedtak(size: Int) {
        antallVedtakHistogram.register(this).record(size.toDouble())
    }
}
