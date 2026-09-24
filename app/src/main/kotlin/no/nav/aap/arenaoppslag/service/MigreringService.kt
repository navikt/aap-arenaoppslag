package no.nav.aap.arenaoppslag.service

import no.nav.aap.arenaoppslag.database.MeldekortperiodeRepository
import no.nav.aap.arenaoppslag.database.VedtakRepository
import no.nav.aap.arenaoppslag.modeller.ArenaSak
import no.nav.aap.arenaoppslag.modeller.PersonId
import no.nav.aap.arenaoppslag.modeller.SakId
import no.nav.aap.arenaoppslag.modeller.migrering.Krav
import java.time.LocalDate

class MigreringService(
    private val vedtakRepository: VedtakRepository,
    private val meldekortperiodeRepository: MeldekortperiodeRepository,
    private val telleverkService: TelleverkService,
) {

    fun hentKravForSak(sak: ArenaSak, sakId: SakId, idag: LocalDate = LocalDate.now()): Krav {
        val forsteInnvilgedeVedtak = vedtakRepository.hentForsteInnvilgetVedtakForSak(sakId)
        val gjeldendeMeldekortperiode = meldekortperiodeRepository.hentGjeldendePeriode(idag)
        val migreringsdato = gjeldendeMeldekortperiode?.fraOgMedDato

        val gjenstaaendeOrdinaerKvote = migreringsdato?.let {
            hentGjenstaaendeOrdinaerKvote(PersonId(sak.person.personId), it)
        }

        return Krav(
            lopenr = sak.lopenr,
            aar = sak.opprettetAar,
            soknadsdato = forsteInnvilgedeVedtak?.fraOgMed,
            migreringsdato = migreringsdato,
            gjenstaaendeOrdinaerKvote = gjenstaaendeOrdinaerKvote,
        )
    }

    private fun hentGjenstaaendeOrdinaerKvote(personId: PersonId, somAv: LocalDate): Int? =
        telleverkService.hentKvoteBrukHendelserForPerson(personId)
            .filter { it.kvoteTypeKode == KVOTE_ORDINAER && !it.datoHendelse.isAfter(somAv) }
            .maxByOrNull { it.id }
            ?.resterende

    private companion object {
        private const val KVOTE_ORDINAER = "AAP"
    }
}
