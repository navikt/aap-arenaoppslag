package no.nav.aap.arenaoppslag.kontrakt.apiv1

import java.time.LocalDate


public data class MaksdatoSamordningRequest(
    val personidentifikator: String
)

public enum class SamordningVedtakStatus {
    LØPENDE, STANSET, AVSLUTTET, ANNET;

    public companion object {
        public fun fromArenaKode(vedtak: VedtakMedMaksdato): SamordningVedtakStatus {
            if (vedtak.vedtakstatuskode == "AVSLU") {
                return AVSLUTTET
            }
            return when (vedtak.vedtaktypeKode) {
                "O", "E", "G" -> LØPENDE
                "S" -> STANSET
                else -> ANNET
            }
        }
    }
}

public data class SamordningSisteVedtak(
    val vedtakId: Int,
    val saknummer: String,
    val maksdato: LocalDate?, // nullable hvis verdien ikke er definert i Arena (f.eks. for stansede vedtak)
    val status: SamordningVedtakStatus,
    val harForlengelseEtter11_12: Boolean
)

public data class MaksdatoSamordningResponse(
    val sisteAktuelleVedtak: SamordningSisteVedtak? // null dersom ingen slike finnes for personen
) {
    public companion object {
        public fun from(sakMedSisteVedtakOgMaksdato: SakMedSisteVedtakOgMaksdato?): MaksdatoSamordningResponse {
            return sakMedSisteVedtakOgMaksdato?.let {
                MaksdatoSamordningResponse(
                    SamordningSisteVedtak(
                        vedtakId = it.sisteVedtak.vedtakId,
                        saknummer = it.saknummer,
                        maksdato = it.sisteVedtak.maxdatoUnntak ?: it.sisteVedtak.maxdatoOrdinaer,
                        status = SamordningVedtakStatus.fromArenaKode(it.sisteVedtak),
                        harForlengelseEtter11_12 = it.unntaksvilkaarInnvilget == true,
                    )
                )
            } ?: INGEN
        }

        public val INGEN: MaksdatoSamordningResponse = MaksdatoSamordningResponse(null)
    }
}
