package no.nav.aap.arenaoppslag.kontrakt.apiv1

import java.time.LocalDate

public data class MaksdatoSamordningRequest(
    val personidentifikator: String
)

public data class MaksdatoSamordningResponse(
    val sisteAktuelleVedtak: SamordningSisteVedtak? // null dersom ingen slike finnes for personen
) {
    public companion object {
        public fun from(sakMedSisteVedtakOgMaksdato: SakMedSisteVedtakOgMaksdato?): MaksdatoSamordningResponse {
            return sakMedSisteVedtakOgMaksdato?.let {
                val status = SamordningVedtakStatus.fromArenaKode(it.sisteVedtak)
                // Vi vet ikke hvor lenge stansen varer, så vi kan ikke vite maksdato for stansede vedtak
                val maksdatoErDefinert = status != SamordningVedtakStatus.STANSET
                val maksdato = if (maksdatoErDefinert) {
                    it.sisteVedtak.maxdatoUnntak ?: it.sisteVedtak.maxdatoOrdinaer
                } else null

                MaksdatoSamordningResponse(
                    SamordningSisteVedtak(
                        vedtakId = it.sisteVedtak.vedtakId,
                        saknummer = it.saknummer,
                        maksdato = maksdato,
                        status = status,
                        harForlengelseEtter11_12 = it.unntaksvilkaarInnvilget == true,
                    )
                )
            } ?: INGEN
        }

        public val INGEN: MaksdatoSamordningResponse = MaksdatoSamordningResponse(null)
    }
}

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
    val maksdato: LocalDate?, // null hvis verdien ikke er definert i Arena
    val status: SamordningVedtakStatus,
    val harForlengelseEtter11_12: Boolean
)
