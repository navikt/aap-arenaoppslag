package no.nav.aap.arenaoppslag.service


import no.nav.aap.arenaoppslag.database.TelleverkRepository
import no.nav.aap.arenaoppslag.modeller.TellerverkPåPerson

class TelleverkService(private val telleverkRepository: TelleverkRepository)
{
        fun hentTelleverkPåPerson(fodselsnr: String): TellerverkPåPerson? {
            val tellekvoter = telleverkRepository.hentTelleverkPåPerson(fodselsnr)
            val ordinaerAAPKvote = tellekvoter.find { it.kode == "AAP" }?.verdi ?: return null
            val utvidetAAPKvote = tellekvoter.find { it.kode == "MAAPU" }?.verdi ?: return null
            return TellerverkPåPerson(
                ordineerAAPKvote = ordinaerAAPKvote,
                utvidetAAPKvote = utvidetAAPKvote
            )
        }
}