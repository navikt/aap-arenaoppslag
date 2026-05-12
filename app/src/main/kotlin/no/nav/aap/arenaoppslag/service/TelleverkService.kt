package no.nav.aap.arenaoppslag.service

import no.nav.aap.arenaoppslag.database.TelleverkRepository
import no.nav.aap.arenaoppslag.modeller.PersonId
import no.nav.aap.arenaoppslag.modeller.TelleverkForPerson

class TelleverkService(private val telleverkRepository: TelleverkRepository) {
    fun hentTelleverkForPerson(personId: PersonId): TelleverkForPerson {
        val tellekvoter = telleverkRepository.hentTelleverkForPerson(personId)
        val ordinaerAAPKvote = tellekvoter.firstOrNull { it.kode == "AAP" }.verdi // skal alltid finnes i Arena
        val utvidetAAPKvote = tellekvoter.firstOrNull { it.kode == "MAAPU" }?.verdi
        return TelleverkForPerson(
            ordineerAAPKvote = ordinaerAAPKvote,
            utvidetAAPKvote = utvidetAAPKvote
        )
    }
}
