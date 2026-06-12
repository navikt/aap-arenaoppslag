package no.nav.aap.arenaoppslag.service

import io.mockk.mockk
import no.nav.aap.arenaoppslag.database.SaksopplysningRepository
import no.nav.aap.arenaoppslag.modeller.ArenaSaksopplysning
import no.nav.aap.arenaoppslag.modeller.ArenaSaksopplysningAttributt
import no.nav.aap.arenaoppslag.modeller.AnnenYtelse
import no.nav.aap.arenaoppslag.modeller.AnnenYtelseType
import no.nav.aap.arenaoppslag.modeller.BelopPeriode
import no.nav.aap.arenaoppslag.modeller.InstitusjonOpphold
import no.nav.aap.arenaoppslag.modeller.InstitusjonOppholdType
import no.nav.aap.arenaoppslag.modeller.ReduksjonType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SaksopplysningServiceTest {

    private val repo = mockk<SaksopplysningRepository>()
    private val service = SaksopplysningService(repo)

    @Test
    fun `hentSamordningOgInstitusjon returnerer tomme lister nar ingen saksopplysninger`() {
        val resultat = service.hentSamordningOgInstitusjon(emptyList())

        assertThat(resultat.institusjonOpphold).isEmpty()
        assertThat(resultat.andreYtelser).isEmpty()
    }

    @Test
    fun `hentSamordningOgInstitusjon filtrerer ut saksopplysninger med ukjente koder`() {
        val saksopplysninger = listOf(lagSaksopplysning(kode = "ARBEVNE"))

        val resultat = service.hentSamordningOgInstitusjon(saksopplysninger)

        assertThat(resultat.institusjonOpphold).isEmpty()
        assertThat(resultat.andreYtelser).isEmpty()
    }

    @Test
    fun `hentSamordningOgInstitusjon mapper AAOKYT-saksopplysning til AnnenYtelse`() {
        // Arena-eksempel: saksopplysningkode er alltid AAOKYT, type hentes fra TYPE-attributt
        val saksopplysninger = listOf(
            lagSaksopplysning(
                kode = AnnenYtelse.SAKSOPPLYSNINGKODE,
                attributter = listOf(
                    lagAttributt(AnnenYtelse.ATTRIBUTT_TYPE, AnnenYtelseType.UFORETRYGD.kode),
                    lagAttributt(AnnenYtelse.ATTRIBUTT_BELOP_PERIODE, BelopPeriode.MND.kode),
                    lagAttributt(AnnenYtelse.ATTRIBUTT_GRAD, "50"),
                    lagAttributt(AnnenYtelse.ATTRIBUTT_BELOP, "0"),
                ),
            ),
        )

        val resultat = service.hentSamordningOgInstitusjon(saksopplysninger)

        assertThat(resultat.andreYtelser).hasSize(1)
        assertThat(resultat.institusjonOpphold).isEmpty()
        val ytelse = resultat.andreYtelser.single()
        assertThat(ytelse.type).isEqualTo(AnnenYtelseType.UFORETRYGD)
        assertThat(ytelse.belopPeriode).isEqualTo(BelopPeriode.MND)
        assertThat(ytelse.grad).isEqualTo("50")
        assertThat(ytelse.beløp).isEqualTo("0")
    }

    @Test
    fun `hentSamordningOgInstitusjon utelater AAOKYT nar TYPE-attributt mangler`() {
        val saksopplysninger = listOf(
            lagSaksopplysning(kode = AnnenYtelse.SAKSOPPLYSNINGKODE, attributter = emptyList()),
        )

        val resultat = service.hentSamordningOgInstitusjon(saksopplysninger)

        assertThat(resultat.andreYtelser).isEmpty()
    }

    @Test
    fun `hentSamordningOgInstitusjon mapper INSOPPH til InstitusjonOpphold for helseinstitusjon`() {
        // INSTA=J aktiverer helseinstitusjon; datoformat fra Arena er dd-MM-yyyy
        val saksopplysninger = listOf(
            lagSaksopplysning(
                kode = InstitusjonOpphold.SAKSOPPLYSNINGKODE,
                attributter = listOf(
                    lagAttributt(InstitusjonOpphold.ATTRIBUTT_INSTA, "J"),
                    lagAttributt(InstitusjonOpphold.ATTRIBUTT_FRA, "01-01-2024"),
                    lagAttributt(InstitusjonOpphold.ATTRIBUTT_TIL, "31-12-2024"),
                    lagAttributt(InstitusjonOpphold.ATTRIBUTT_FRI_KOST_LOSJI, "J"),
                    lagAttributt(InstitusjonOpphold.ATTRIBUTT_REDUKSJON, "RED00"),
                ),
            ),
        )

        val resultat = service.hentSamordningOgInstitusjon(saksopplysninger)

        assertThat(resultat.institusjonOpphold).hasSize(1)
        assertThat(resultat.andreYtelser).isEmpty()
        val opphold = resultat.institusjonOpphold.single()
        assertThat(opphold.type).isEqualTo(InstitusjonOppholdType.Helseinstitusjon)
        assertThat(opphold.fra).isEqualTo(LocalDate.of(2024, 1, 1))
        assertThat(opphold.til).isEqualTo(LocalDate.of(2024, 12, 31))
        assertThat(opphold.friKostOgLosji).isTrue()
        assertThat(opphold.reduksjonsType).isEqualTo(ReduksjonType.INGEN)
    }

    @Test
    fun `hentSamordningOgInstitusjon mapper INSOPPH til InstitusjonOpphold for straffegjennomforing`() {
        // STRFG=J aktiverer straffegjennomføring
        val saksopplysninger = listOf(
            lagSaksopplysning(
                kode = InstitusjonOpphold.SAKSOPPLYSNINGKODE,
                attributter = listOf(
                    lagAttributt(InstitusjonOpphold.ATTRIBUTT_STRAFFEGJENNOMFORING, "J"),
                    lagAttributt(InstitusjonOpphold.ATTRIBUTT_FRA, "01-03-2023"),
                ),
            ),
        )

        val resultat = service.hentSamordningOgInstitusjon(saksopplysninger)

        assertThat(resultat.institusjonOpphold).hasSize(1)
        val opphold = resultat.institusjonOpphold.single()
        assertThat(opphold.type).isEqualTo(InstitusjonOppholdType.FENGSEL)
        assertThat(opphold.fra).isEqualTo(LocalDate.of(2023, 3, 1))
        assertThat(opphold.til).isNull()
        assertThat(opphold.friKostOgLosji).isFalse()
    }

    @Test
    fun `hentSamordningOgInstitusjon utelater INSOPPH nar bade STRFG og INSTA er N`() {
        // Inaktiv post (verdi N på begge) skal ikke tas med, som i eksemplet fra Arena
        val saksopplysninger = listOf(
            lagSaksopplysning(
                kode = InstitusjonOpphold.SAKSOPPLYSNINGKODE,
                attributter = listOf(
                    lagAttributt(InstitusjonOpphold.ATTRIBUTT_STRAFFEGJENNOMFORING, "N"),
                    lagAttributt(InstitusjonOpphold.ATTRIBUTT_INSTA, "N"),
                    lagAttributt(InstitusjonOpphold.ATTRIBUTT_FRA, "01-01-2024"),
                ),
            ),
        )

        val resultat = service.hentSamordningOgInstitusjon(saksopplysninger)

        assertThat(resultat.institusjonOpphold).isEmpty()
    }

    @Test
    fun `hentSamordningOgInstitusjon utelater InstitusjonOpphold uten fra-dato`() {
        val saksopplysninger = listOf(
            lagSaksopplysning(
                kode = InstitusjonOpphold.SAKSOPPLYSNINGKODE,
                attributter = listOf(lagAttributt(InstitusjonOpphold.ATTRIBUTT_INSTA, "J")),
            ),
        )

        val resultat = service.hentSamordningOgInstitusjon(saksopplysninger)

        assertThat(resultat.institusjonOpphold).isEmpty()
    }

    @Test
    fun `hentSamordningOgInstitusjon returnerer begge typer nar begge finnes`() {
        val saksopplysninger = listOf(
            lagSaksopplysning(
                kode = InstitusjonOpphold.SAKSOPPLYSNINGKODE,
                attributter = listOf(
                    lagAttributt(InstitusjonOpphold.ATTRIBUTT_STRAFFEGJENNOMFORING, "J"),
                    lagAttributt(InstitusjonOpphold.ATTRIBUTT_FRA, "01-03-2023"),
                ),
            ),
            lagSaksopplysning(
                kode = AnnenYtelse.SAKSOPPLYSNINGKODE,
                attributter = listOf(lagAttributt(AnnenYtelse.ATTRIBUTT_TYPE, AnnenYtelseType.BARNEPENSJON.kode)),
            ),
        )

        val resultat = service.hentSamordningOgInstitusjon(saksopplysninger)

        assertThat(resultat.institusjonOpphold).hasSize(1)
        assertThat(resultat.andreYtelser).hasSize(1)
    }

    private fun lagSaksopplysning(
        kode: String,
        verdi: String? = null,
        attributter: List<ArenaSaksopplysningAttributt> = emptyList(),
    ) = ArenaSaksopplysning(
        saksopplysningId = 1L,
        saksopplysningkode = kode,
        saksopplysningnavn = kode,
        skjermbildetekst = null,
        statusRepeterbar = "N",
        verdi = verdi,
        attributter = attributter,
    )

    private fun lagAttributt(kode: String, verdi: String?) = ArenaSaksopplysningAttributt(
        attributtkode = kode,
        skjermbildetekst = null,
        formatnavn = null,
        posisjon = 1,
        verdi = verdi,
        statusSjekketAv = null,
    )
}
