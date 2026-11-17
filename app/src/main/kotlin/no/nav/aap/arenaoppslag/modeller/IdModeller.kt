package no.nav.aap.arenaoppslag.modeller

data class ArenaPersonId(val id: Int)

data class PersonIdentifikator(val id: String) {
    companion object {
        fun fromString(id: String?): PersonIdentifikator? {
            // Et fødselsnummer eller d-nummer
            if (id == null || id.length != 11 || !id.all { it.isDigit() }) return null

            return PersonIdentifikator(id)
        }
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
