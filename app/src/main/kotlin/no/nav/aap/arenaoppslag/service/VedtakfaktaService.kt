package no.nav.aap.arenaoppslag.service

import no.nav.aap.arenaoppslag.database.VedtakfaktaRepository
import no.nav.aap.arenaoppslag.kontrakt.apiv1.ArenaVedtakfakta

class VedtakfaktaService(
    private val vedtakfaktaRepository: VedtakfaktaRepository
) {
    fun hentForVedtakliste(vedtakIdListe: List<Int>): Map<Int, List<ArenaVedtakfakta>> =
        vedtakfaktaRepository.hentForVedtakIder(vedtakIdListe)
            .mapValues { (_, fakta) -> fakta.map { it.tilKontrakt() } }

}
