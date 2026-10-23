package no.nav.aap.arenaoppslag.modeller

import no.nav.aap.arenaoppslag.kontrakt.apiv1.SakMedSisteVedtakOgMaksdato
import no.nav.aap.arenaoppslag.kontrakt.apiv1.TelleverkForPerson
import java.time.LocalDate

data class HistorikkStatusDetaljer(
    val telleverk: TelleverkForPerson?,
    val maksdato: SakMedSisteVedtakOgMaksdato?,
    val sisteUtbetalingDato: LocalDate?,
)
