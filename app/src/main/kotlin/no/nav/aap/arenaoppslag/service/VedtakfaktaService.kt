package no.nav.aap.arenaoppslag.service

import no.nav.aap.arenaoppslag.database.VedtakfaktaRepository
import no.nav.aap.arenaoppslag.modeller.ArenaVedtakfakta
import no.nav.aap.arenaoppslag.modeller.VedtakId

class VedtakfaktaService(private val vedtakfaktaRepository: VedtakfaktaRepository) {
    fun hentVedtakfaktaForVedtak(vedtakId: VedtakId): List<ArenaVedtakfakta> =
        vedtakfaktaRepository.hentForVedtakId(vedtakId)
}

