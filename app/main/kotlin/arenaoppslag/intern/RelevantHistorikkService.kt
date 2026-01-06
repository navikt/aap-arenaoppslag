package arenaoppslag.intern

import no.nav.aap.arenaoppslag.kontrakt.intern.PersonHarSignifikantAAPArenaHistorikk
import java.time.LocalDate

class RelevantHistorikkService(private val arenaRepository: ArenaRepository){
    fun hentRelevanteSakerForPerson(personIdentifikatorer: List<String>, virkningstidspunkt: LocalDate): PersonHarSignifikantAAPArenaHistorikk {
        val arenaData = arenaRepository.hentKanBehandlesIKelvin(personIdentifikatorer, virkningstidspunkt)

        return PersonHarSignifikantAAPArenaHistorikk(arenaData.kanBehandles, arenaData.arenaSakIdListe)
    }

}
