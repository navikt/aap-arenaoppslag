package no.nav.aap.arenaoppslag.service

import no.nav.aap.arenaoppslag.database.MedisinskOpplysningRepository
import no.nav.aap.arenaoppslag.database.MeldekortperiodeRepository
import no.nav.aap.arenaoppslag.database.VedtakRepository
import no.nav.aap.arenaoppslag.database.VilkårsvurderingRepository
import no.nav.aap.arenaoppslag.modeller.ArenaSak
import no.nav.aap.arenaoppslag.modeller.PersonId
import no.nav.aap.arenaoppslag.modeller.SakId
import no.nav.aap.arenaoppslag.modeller.migrering.Krav
import no.nav.aap.arenaoppslag.modeller.migrering.Sykdomsvurdering
import java.time.LocalDate

class MigreringService(
    private val vedtakRepository: VedtakRepository,
    private val meldekortperiodeRepository: MeldekortperiodeRepository,
    private val telleverkService: TelleverkService,
    private val vilkårsvurderingRepository: VilkårsvurderingRepository,
    private val medisinskOpplysningRepository: MedisinskOpplysningRepository,
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

    fun hentSykdomsvurderingForSak(
        sak: ArenaSak,
        sakId: SakId,
        idag: LocalDate = LocalDate.now(),
    ): Sykdomsvurdering? {
        val vedtak = vedtakRepository.hentGjeldende115VedtakForSak(sakId, idag) ?: return null
        val vilkårsvurderinger = vilkårsvurderingRepository.hentForVedtakIder(listOf(vedtak.vedtakId))[vedtak.vedtakId]
            .orEmpty()
        val diagnoser = medisinskOpplysningRepository.hentForPerson(PersonId(sak.person.personId))

        return Sykdomsvurdering(
            vedtakId = vedtak.vedtakId,
            begrunnelse = vedtak.begrunnelse,
            vilkar = vilkårsvurderinger.filter { it.vilkårkode in setOf("INNTNEDS", "SYKSKADLYT", "AAARBEVNE") },
            diagnoser = diagnoser,
        )
    }

    private fun hentGjenstaaendeOrdinaerKvote(personId: PersonId, migreringsdato: LocalDate): Int? {
        val hendelser = telleverkService.hentKvoteBrukHendelserForPerson(personId)
            .filter { it.kvoteTypeKode == KVOTE_ORDINAER }
            .sortedBy { it.id }
        val siste = hendelser.lastOrNull() ?: return null

        // RESTERENDE er en løpende sum fra siste INIT/NULLE, så kun bevegelser etter den påvirker saldoen.
        val sisteNullstilling = hendelser.lastOrNull { it.posteringTypeKode in NULLSTILLINGSTYPER }?.id ?: Int.MIN_VALUE
        val meldekorttrekk = hendelser.filter { it.id > sisteNullstilling && it.endringsGrunnlag == GRUNNLAG_MELDEKORT }
        val meldeperioder = meldekortperiodeRepository.hentPerioderForMeldekort(
            meldekorttrekk.map { it.objektIdGrunnlag }
        )

        val trekkEtterMigrering = meldekorttrekk
            .filter { meldeperioder[it.objektIdGrunnlag]?.fraOgMedDato?.isBefore(migreringsdato) == false }
            .sumOf { it.antallBevegelse }

        return (siste.resterende - trekkEtterMigrering) / KVOTEENHETER_PER_DAG
    }

    private companion object {
        private const val KVOTE_ORDINAER = "AAP"
        private const val GRUNNLAG_MELDEKORT = "MKORT"
        private val NULLSTILLINGSTYPER = setOf("INIT", "NULLE")
        // Arena lagrer kvoten i enheter der én hel dag = 20 (100 per uke), f.eks. 15680 = 784 dager.
        // Delvise dager rundes ned fordi Krav-kontrakten bruker hele dager.
        private const val KVOTEENHETER_PER_DAG = 20
    }
}
