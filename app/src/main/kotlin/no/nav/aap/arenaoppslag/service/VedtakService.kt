package no.nav.aap.arenaoppslag.service

import no.nav.aap.arenaoppslag.database.VedtakRepository
import no.nav.aap.arenaoppslag.database.VedtakfaktaRepository
import no.nav.aap.arenaoppslag.modeller.ArenaVedtakMedMaxdato
import no.nav.aap.arenaoppslag.modeller.PersonId

class VedtakService(
    private val vedtakRepository: VedtakRepository,
    private val vedtakfaktaRepository: VedtakfaktaRepository,
) {
    fun hentSisteLøpendeVedtakDetalj(personId: PersonId): ArenaVedtakMedMaxdato? {
        val sakMedVedtak = vedtakRepository.hentSisteLøpendeVedtakDetalj(personId)

        val vedtakfakta = sakMedVedtak?.vedtakId?.let { vedtakId ->
            vedtakfaktaRepository.hentForVedtakIder(listOf(vedtakId))[vedtakId]
        }
        val unntakInnvilget = vedtakfakta?.firstOrNull { it.kode == "UNNTAKAAP" }?.somBooleanVerdi()
        val unntaksdato = vedtakfakta?.firstOrNull { it.kode == "AAPVILKUNN" }?.somDatoVerdi()

        return sakMedVedtak?.copy(
            unntaksvilkaarGjelderFra = unntaksdato,
            unntaksvilkaarInnvilget = unntakInnvilget,
        )
    }
}
