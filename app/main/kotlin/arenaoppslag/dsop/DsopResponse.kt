package arenaoppslag.dsop

import no.nav.aap.arenaoppslag.kontrakt.dsop.Periode

data class VedtakResponse(
    val uttrekksperiode: Periode,
    val vedtaksliste: List<DsopVedtak>
) {
    fun tilKontrakt(): no.nav.aap.arenaoppslag.kontrakt.dsop.VedtakResponse {
        return no.nav.aap.arenaoppslag.kontrakt.dsop.VedtakResponse(
            uttrekksperiode,
            vedtaksliste.map { it.tilKontrakt() })
    }
}

data class MeldekortResponse(
    val uttrekksperiode: Periode,
    val meldekortliste: List<DsopMeldekort>
) {
    fun tilKontrakt(): no.nav.aap.arenaoppslag.kontrakt.dsop.MeldekortResponse {
        return no.nav.aap.arenaoppslag.kontrakt.dsop.MeldekortResponse(
            uttrekksperiode,
            meldekortliste.map { it.tilKontrakt() })
    }
}

data class DsopVedtak(
    val vedtakId: Int,
    val virkningsperiode: Periode,
    val vedtakstype: Kodeverdi,
    val vedtaksvariant: Kodeverdi,
    val vedtakstatus: Kodeverdi,
    val rettighetstype: Kodeverdi,
    val utfall: Kodeverdi,
    val aktivitetsfase: Kodeverdi,
) {
    fun tilKontrakt(): no.nav.aap.arenaoppslag.kontrakt.dsop.DsopVedtak {
        return no.nav.aap.arenaoppslag.kontrakt.dsop.DsopVedtak(
            vedtakId,
            virkningsperiode,
            vedtakstype.tilKontrakt(),
            vedtaksvariant.tilKontrakt(),
            vedtakstatus.tilKontrakt(),
            rettighetstype.tilKontrakt(),
            utfall.tilKontrakt(),
            aktivitetsfase.tilKontrakt()
        )
    }
}

data class DsopMeldekort(
    val meldekortId: Int,
    val periode: Periode,
    val antallTimerArbeidet: Double
) {
    fun tilKontrakt(): no.nav.aap.arenaoppslag.kontrakt.dsop.DsopMeldekort {
        return no.nav.aap.arenaoppslag.kontrakt.dsop.DsopMeldekort(
            meldekortId,
            periode,
            antallTimerArbeidet
        )
    }
}

data class Kodeverdi(
    val kode: String,
    val termnavn: String
) {
    fun tilKontrakt(): no.nav.aap.arenaoppslag.kontrakt.dsop.Kodeverdi {
        return no.nav.aap.arenaoppslag.kontrakt.dsop.Kodeverdi(kode, termnavn)
    }
}
