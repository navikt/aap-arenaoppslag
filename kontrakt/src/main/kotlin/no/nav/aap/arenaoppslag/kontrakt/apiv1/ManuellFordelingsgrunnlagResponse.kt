package no.nav.aap.arenaoppslag.kontrakt.apiv1

import java.time.LocalDate

public data class ManuellFordelingsgrunnlagRequest(
    val personidentifikator: String,
)

/**
 * Sammensatt AAP-grunnlag for manuell vurdering av søknad i postmottak/Kelvin.
 *
 * Alle felter er nullable fordi personen kan finnes i Arena uten aktuelle AAP-vedtak.
 * Felter for andre ytelser og oppgaver i Arena kommer senere.
 */
public data class ManuellFordelingsgrunnlagResponse(
    val saksnummer: String?,
    val erAktiv: Boolean,
    val under52Uker: Boolean?,
    val gjenståendeOrdinæreDager: Int?,
    // Samlet gjenstående unntaksperiode §11-12 (andre og tredje ledd slås sammen).
    val gjenståendeUnntaksDager: Int?,
    val sisteVedtak: VedtakMedMaksdato?,
    val sisteUtbetaling: LocalDate?,
)

