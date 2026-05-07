package no.nav.aap.arenaoppslag.service

import no.nav.aap.arenaoppslag.database.TelleverkRepository
import no.nav.aap.arenaoppslag.modeller.PersonId
import no.nav.aap.arenaoppslag.modeller.TellerverkPåPerson

class TelleverkService(private val telleverkRepository: TelleverkRepository) {
    fun hentTelleverkPåPerson(personId: PersonId): TellerverkPåPerson? {
        val tellekvoter = telleverkRepository.hentTelleverkPåPerson(personId)
        val ordinaerAAPKvote = tellekvoter.find { it.kode == "AAP" }?.verdi ?: return null
        val utvidetAAPKvote = tellekvoter.find { it.kode == "MAAPU" }?.verdi ?: return null
        return TellerverkPåPerson(
            ordineerAAPKvote = ordinaerAAPKvote,
            utvidetAAPKvote = utvidetAAPKvote
        )
    }
}
