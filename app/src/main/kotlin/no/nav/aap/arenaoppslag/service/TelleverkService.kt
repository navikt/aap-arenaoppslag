package no.nav.aap.arenaoppslag.service

import no.nav.aap.arenaoppslag.database.TelleverkRepository
import no.nav.aap.arenaoppslag.modeller.PersonId
import no.nav.aap.arenaoppslag.modeller.TelleverkForPerson

class TelleverkService(private val telleverkRepository: TelleverkRepository) {
    fun hentTelleverkForPerson(personId: PersonId): TelleverkForPerson? {
        val tellekvoter = telleverkRepository.hentTelleverkForPerson(personId)
        val ordinaerAAPKvote = tellekvoter.firstOrNull { it.kode == "AAP" }?.verdi
        val utvidetAAPKvote = tellekvoter.firstOrNull { it.kode == "MAAPU" }?.verdi

        // ordinaerAAPKvote skal finnes i alle tilfeller. Men det er enkelte unntak for hvis personen ikke har
        // telleverk i det hele tatt. F.eks. fordi det ikke er fattet noen vedtak på saken til personen.
        if (ordinaerAAPKvote == null) {
            return null
        }

        return TelleverkForPerson(
            ordineerAAPKvote = ordinaerAAPKvote,
            utvidetAAPKvote = utvidetAAPKvote
        )
    }
}
