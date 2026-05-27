package no.nav.aap.arenaoppslag.modeller

data class PersonId(val id: Int)

data class SakId(val id: Int) {
    companion object {
        fun fromString(id: String): SakId? {
            return id.toIntOrNull()?.let { SakId(it) }
        }
    }
}

data class Saksnummer(val lopenummer: Int, val aar: Int) {
    companion object {
        fun fromString(id: String): Saksnummer? {
            if (!id.contains('-')) {
                return null;
            }

            val (aar, lopenr) = id.split('-')
            return Saksnummer(lopenummer = lopenr.toInt(), aar = aar.toInt())
        }
    }
}
