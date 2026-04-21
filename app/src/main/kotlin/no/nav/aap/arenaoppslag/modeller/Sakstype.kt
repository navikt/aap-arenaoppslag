package no.nav.aap.arenaoppslag.modeller

import no.nav.aap.arenaoppslag.kontrakt.apiv1.SakstypeKontrakt


enum class Sakstype(val sakskode: String, val visningsnavn: String) {
    AA("AA", "Arbeidsavklaringspenger"),
    ARBEID("ARBEID", "Oppfølgingssak"),
    ATTF("ATTF", "Yrkesrettet attføring"),
    DAGP("DAGP", "Dagpenger"),
    ENSLIG("ENSLIG", "Enslig forsørger"),
    FEILUTBE("FEILUTBE", "Feilutbetaling"),
    INDIV("INDIV", "Individstønad"),
    KLAN("KLAN", "Klage/Anke"),
    MOBIL("MOBIL", "Mobilitetsfremmende stønad"),
    REHAB("REHAB", "Rehabiliteringspenger"),
    SANKSJON("SANKSJON", "Sanksjon sykmeldt"),
    SANKSJON_A("SANKSJON_A", "Sanksjon arbeidsgiver"),
    SANKSJON_B("SANKSJON_B", "Sanksjon behandler"),
    SYKEP("SYKEP", "Sykepenger"),
    TILSTOVER("TILSTOVER", "Tilleggsstønad"),
    TILSTRAMME("TILSTRAMME", "Tilleggsstønad arbeidssøkere"),
    TILT("TILT", "Tiltakssak"),
    UFOREYT("UFOREYT", "Uføreytelser"),
    UTRSYA("UTRSYA", "Utredning KTD"),
    VLONN("VLONN", "Ventelønn");

    companion object {
        private val fraKode = entries.associateBy { it.sakskode }

        // Sakstyper fra Arena kan i teorien inneholde ukjente koder, så vi returnerer null istedenfor å kaste exception
        fun fraKode(kode: String): Sakstype? = fraKode[kode]
    }

    fun tilKontrakt(): SakstypeKontrakt = when (this) {
        AA -> SakstypeKontrakt.AA
        ARBEID -> SakstypeKontrakt.ARBEID
        ATTF -> SakstypeKontrakt.ATTF
        DAGP -> SakstypeKontrakt.DAGP
        ENSLIG -> SakstypeKontrakt.ENSLIG
        FEILUTBE -> SakstypeKontrakt.FEILUTBE
        INDIV -> SakstypeKontrakt.INDIV
        KLAN -> SakstypeKontrakt.KLAN
        MOBIL -> SakstypeKontrakt.MOBIL
        REHAB -> SakstypeKontrakt.REHAB
        SANKSJON -> SakstypeKontrakt.SANKSJON
        SANKSJON_A -> SakstypeKontrakt.SANKSJON_A
        SANKSJON_B -> SakstypeKontrakt.SANKSJON_B
        SYKEP -> SakstypeKontrakt.SYKEP
        TILSTOVER -> SakstypeKontrakt.TILSTOVER
        TILSTRAMME -> SakstypeKontrakt.TILSTRAMME
        TILT -> SakstypeKontrakt.TILT
        UFOREYT -> SakstypeKontrakt.UFOREYT
        UTRSYA -> SakstypeKontrakt.UTRSYA
        VLONN -> SakstypeKontrakt.VLONN
    }
}

