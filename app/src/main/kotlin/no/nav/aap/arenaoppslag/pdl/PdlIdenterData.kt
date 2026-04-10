package no.nav.aap.arenaoppslag

data class PdlIdenterData(
    val hentIdenter: PdlIdenter?,
)

data class PdlIdenter(
    val identer: List<PdlIdent>,
)

data class PdlIdent(
    val ident: String,
    val historisk: Boolean,
    val gruppe: String,
)
