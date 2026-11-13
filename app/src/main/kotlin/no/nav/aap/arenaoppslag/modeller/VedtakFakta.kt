package no.nav.aap.arenaoppslag.modeller

/**
 * @property dagsmbt Dagsats med barnetillegg.
 * @property dags Dagsats.
 * @property dagsfsam Dagsats uten barnetillegg før samordning
 * @property barnmston Antall barn med stønad.
 * @property barntill Barnetillegg. Beskrivelsen i vedtakfakta-tabellen sier "barnetillegg per uke", men jeg tror det bare er for dagpenger.
 */
data class VedtakFakta(
    var dagsmbt: Int,
    var dags: Int,
    var barntill: Int,
    var barnmston: Int,
    var dagsfsam: Int,
    var justertg: String?,
)
