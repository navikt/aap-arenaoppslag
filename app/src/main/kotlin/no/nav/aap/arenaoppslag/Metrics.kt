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
        this.counter(
            "arenaoppslag_signifikant_vedtak",
            taggListeForVedtak(vedtak)
        ).also { counter -> counter.increment() }
    }

    private fun taggListeForVedtak(ettVedtak: ArenaVedtak): List<Tag> = listOf(
        Tag.of("type", ettVedtak.vedtaktypeKode ?: "null"),
        Tag.of("rettighet", ettVedtak.rettighetkode),
        Tag.of("status", ettVedtak.statusKode),
        Tag.of("utfall", ettVedtak.utfallkode ?: "null")
    )


    fun MeterRegistry.registrerNyesteSignifikanteVedtakMedAntall(
        siste: ArenaVedtak,
        antall: Int
    ) {
        this.counter(
            "arenaoppslag_signifikante_vedtak_med_antall",
            taggListeForVedtak(siste) + listOf(Tag.of("antall", antall.toString()))
        ).also { counter -> counter.increment() }

    }

}
