package no.nav.aap.arenaoppslag.modeller

data class ArenaPersonId(val id: Int)

data class PersonIdentifikator(val id: String) {
    // Et fødselsnummer eller d-nummer
    init {
        require(id.length == 11) { "Personidentifikatoren må være 11 tegn lang" }
        require(id.all { it.isDigit() }) { "Personidentifikatoren må bare inneholde sifre" }
    }
}

data class SakId(val id: Int) {
    companion object {
        fun fromString(id: String): SakId? {
            return id.toIntOrNull()?.let { SakId(it) }
        }
    }
}

data class Saksnummer(val lopenummerSak: Int, val aar: Int) {
    companion object {
        fun fromString(id: String?): Saksnummer? {
            if(id == null) return null

            if (!id.contains('-')) {
                return null;
            }

            val (aar, lopenr) = id.split('-')
            return Saksnummer(lopenummerSak = lopenr.toInt(), aar = aar.toInt())
        }
    }

    override fun toString(): String {
        return "${aar}-${lopenummerSak}"
    }
}
