package no.nav.aap.arenaoppslag.kontrakt.apiv1

import java.time.LocalDate


public enum class SamordningVedtakStatus {
    LOPENDE, STANSET, OPPHORT, ANNET;

    public companion object {
        public fun fromArenaKode(vedtaktypeKode: String): SamordningVedtakStatus {
            return when (vedtaktypeKode) {
                "O", "E", "G" -> LOPENDE
                "S" -> STANSET
                else -> ANNET
            }
        }
    }
}

public data class MaksdatoSamordningResponse(
    val harAktuelleVedtak: Boolean,
    val maksdato: LocalDate?,
    val harForlengelseEtter11_12: Boolean?,
    val saknummer: String?,
    val status: SamordningVedtakStatus?,
    val vedtakId: Int?
) {
    public companion object {
        public fun from(sakMedSisteVedtakOgMaksdato: SakMedSisteVedtakOgMaksdato?): MaksdatoSamordningResponse {
            return sakMedSisteVedtakOgMaksdato?.let {
                MaksdatoSamordningResponse(
                    harAktuelleVedtak = true,
                    maksdato = it.sisteVedtak.maxdatoUnntak ?: it.sisteVedtak.maxdatoOrdinaer,
                    harForlengelseEtter11_12 = it.unntaksvilkaarInnvilget == true,
                    saknummer = it.saknummer,
                    status = SamordningVedtakStatus.fromArenaKode(it.sisteVedtak.vedtaktypeKode),
                    vedtakId = it.sisteVedtak.vedtakId,
                )
            } ?: INGEN
        }

        public val INGEN: MaksdatoSamordningResponse = MaksdatoSamordningResponse(
            false, null, false, null, null, null,
        )
    }
}
