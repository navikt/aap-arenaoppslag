package no.nav.aap.arenaoppslag.service

import no.nav.aap.arenaoppslag.database.MedisinskOpplysningRepository
import no.nav.aap.arenaoppslag.database.MeldekortperiodeRepository
import no.nav.aap.arenaoppslag.database.VedtakRepository
import no.nav.aap.arenaoppslag.database.VedtakfaktaRepository
import no.nav.aap.arenaoppslag.database.VilkårsvurderingRepository
import no.nav.aap.arenaoppslag.modeller.ArenaSak
import no.nav.aap.arenaoppslag.modeller.PersonId
import no.nav.aap.arenaoppslag.modeller.SakId
import no.nav.aap.arenaoppslag.modeller.migrering.Krav
import no.nav.aap.arenaoppslag.modeller.migrering.Oppholdskrav
import no.nav.aap.arenaoppslag.modeller.migrering.Refusjonskrav
import no.nav.aap.arenaoppslag.modeller.migrering.RefusjonskravForSak
import no.nav.aap.arenaoppslag.modeller.migrering.Sykdomsvurdering
import no.nav.aap.arenaoppslag.modeller.migrering.Vedtaksperiode
import no.nav.aap.komponenter.httpklient.exception.VerdiIkkeFunnetException
import java.time.LocalDate

class MigreringService(
    private val vedtakRepository: VedtakRepository,
    private val meldekortperiodeRepository: MeldekortperiodeRepository,
    private val telleverkService: TelleverkService,
    private val vilkårsvurderingRepository: VilkårsvurderingRepository,
    private val medisinskOpplysningRepository: MedisinskOpplysningRepository,
    private val vedtakfaktaRepository: VedtakfaktaRepository,
) {

    fun hentKravForSak(sak: ArenaSak, sakId: SakId, idag: LocalDate = LocalDate.now()): Krav {
        val forsteInnvilgedeVedtak = vedtakRepository.hentForsteInnvilgetVedtakForSak(sakId)
        val personId = PersonId(sak.person.personId)
        val gjeldendeMeldekortperiode = meldekortperiodeRepository.hentGjeldendePeriodeForPerson(personId, idag)
        val migreringsdato = gjeldendeMeldekortperiode?.fraOgMedDato
            ?: throw VerdiIkkeFunnetException("Fant ingen meldekortperiode for brukeren som dekker $idag for sak ${sakId.id}, saken kan derfor ikke migreres.")

        val sisteFerdigBehandledeMeldekortperiode = meldekortperiodeRepository.hentSisteLevertePeriodeForPerson(personId)

        val gjenstaaendeOrdinaerKvote = hentGjenstaaendeOrdinaerKvote(personId, migreringsdato)

        return Krav(
            lopenr = sak.lopenr,
            aar = sak.opprettetAar,
            soknadsdato = forsteInnvilgedeVedtak?.fraOgMed,
            migreringsdato = migreringsdato,
            gjenstaaendeOrdinaerKvote = gjenstaaendeOrdinaerKvote,
            sisteFerdigBehandledeMeldekortperiode = sisteFerdigBehandledeMeldekortperiode,
        )
    }

    fun hentSykdomsvurderingForSak(
        sak: ArenaSak,
        sakId: SakId,
        idag: LocalDate = LocalDate.now(),
    ): Sykdomsvurdering? {
        val vedtak = vedtakRepository.hentGjeldende115VedtakForSakPåDato(sakId, idag) ?: return null
        val vilkårsvurderinger = vilkårsvurderingRepository.hentForVedtakIder(listOf(vedtak.vedtakId))[vedtak.vedtakId]
            .orEmpty()
        val diagnoser = medisinskOpplysningRepository.hentForPerson(PersonId(sak.person.personId))

        return Sykdomsvurdering(
            vedtakId = vedtak.vedtakId,
            begrunnelse = vedtak.begrunnelse,
            vilkar = vilkårsvurderinger.filter { it.vilkårkode in setOf(
                VILKÅRKODE_INNTEKT_NEDSATT,
                VILKÅRKODE_SYKDOM_SKADE_LYTE,
                VILKÅRKODE_ARBEIDSEVNE)
            },
            diagnoser = diagnoser,
        )
    }

    fun hentOppholdskravForSak(sakId: SakId, idag: LocalDate = LocalDate.now()): Oppholdskrav? {
        val vedtak = vedtakRepository.hentGjeldendeAapVedtakForSakPåDato(sakId, idag) ?: return null
        val vilkårsvurderinger = vilkårsvurderingRepository.hentForVedtakIder(listOf(vedtak.vedtakId))[vedtak.vedtakId]
            .orEmpty()

        return Oppholdskrav(
            vedtakId = vedtak.vedtakId,
            vilkar = vilkårsvurderinger.firstOrNull { it.vilkårkode == VILKÅRKODE_OPPHOLD_I_NORGE },
        )
    }

    fun hentVedtaksperiodeForSak(sakId: SakId, idag: LocalDate = LocalDate.now()): Vedtaksperiode? {
        val vedtak = vedtakRepository.hentGjeldendeAapVedtakForSakPåDato(sakId, idag) ?: return null

        return Vedtaksperiode(
            vedtakId = vedtak.vedtakId,
            fraDato = vedtak.fraOgMed,
            tilDato = vedtak.tilDato,
        )
    }

    fun hentRefusjonskravForSak(sakId: SakId, idag: LocalDate = LocalDate.now()): RefusjonskravForSak? {
        val vedtak = vedtakRepository.hentGjeldendeAapVedtakForSakPåDato(sakId, idag) ?: return null
        val vedtakfakta = vedtakfaktaRepository.hentForVedtakIder(listOf(vedtak.vedtakId))[vedtak.vedtakId].orEmpty()

        return RefusjonskravForSak(Refusjonskrav.fraVedtakfakta(vedtakfakta))
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
        private const val VILKÅRKODE_OPPHOLD_I_NORGE = "AOINORGE3"
        private const val VILKÅRKODE_INNTEKT_NEDSATT = "INNTNEDS"
        private const val VILKÅRKODE_SYKDOM_SKADE_LYTE = "SYKSKADLYT"
        private const val VILKÅRKODE_ARBEIDSEVNE = "AAARBEVNE"
        private const val KVOTE_ORDINAER = "AAP"
        private const val GRUNNLAG_MELDEKORT = "MKORT"
        private val NULLSTILLINGSTYPER = setOf("INIT", "NULLE")
        // Arena lagrer kvoten i enheter der én hel dag = 20 (100 per uke), f.eks. 15680 = 784 dager.
        // Delvise dager rundes ned fordi Krav-kontrakten bruker hele dager.
        private const val KVOTEENHETER_PER_DAG = 20
    }
}
