package no.nav.aap.arenaoppslag.modeller

data class PersonId(val id: Int)

sealed class SakIdentifikator {
    companion object {
        fun fromString(id: String): SakIdentifikator? {
            return Saksnummer.fromString(id) ?: SakId.fromString(id)
        }
    }
}

data class SakId(val id: Int): SakIdentifikator() {
    companion object {
        fun fromString(id: String): SakId? {
            return id.toIntOrNull()?.let { SakId(it) }
        }
    }
}

data class Saksnummer(val lopenummer: Int, val aar: Int): SakIdentifikator() {
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