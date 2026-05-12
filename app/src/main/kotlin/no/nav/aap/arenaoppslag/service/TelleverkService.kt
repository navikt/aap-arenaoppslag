package no.nav.aap.arenaoppslag.service

import no.nav.aap.arenaoppslag.database.TelleverkRepository
import no.nav.aap.arenaoppslag.modeller.PersonId
import no.nav.aap.arenaoppslag.modeller.TelleverkPåPerson

class TelleverkService(private val telleverkRepository: TelleverkRepository) {
    fun hentTelleverkPåPerson(personId: PersonId): TelleverkPåPerson {
        val tellekvoter = telleverkRepository.hentTelleverkPåPerson(personId)
        val ordinaerAAPKvote = tellekvoter.first { it.kode == "AAP" }.verdi // skal alltid finnes i Arena
        val utvidetAAPKvote = tellekvoter.firstOrNull { it.kode == "MAAPU" }?.verdi
        return TelleverkPåPerson(
            ordineerAAPKvote = ordinaerAAPKvote,
            utvidetAAPKvote = utvidetAAPKvote
        )
    }
}
