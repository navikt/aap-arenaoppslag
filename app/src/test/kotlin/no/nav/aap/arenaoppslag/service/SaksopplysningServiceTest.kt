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
    fun `hentSamordningOgInstitusjon returnerer tom map nar ingen saksopplysninger`() {
        val resultat = service.hentSamordningOgInstitusjon(emptyMap())

        assertThat(resultat).isEmpty()
    }

    @Test
    fun `hentSamordningOgInstitusjon filtrerer ut saksopplysninger med ukjente koder`() {
        val resultat = service.hentSamordningOgInstitusjon(mapOf(1 to listOf(lagSaksopplysning(kode = "ARBEVNE"))))

        assertThat(resultat[1]?.institusjonOpphold).isEmpty()
        assertThat(resultat[1]?.andreYtelser).isEmpty()
    }

    @Test
    fun `hentSamordningOgInstitusjon mapper AAOKYT-saksopplysning til AnnenYtelse`() {
        // Arena-eksempel: saksopplysningkode er alltid AAOKYT, type hentes fra TYPE-attributt
        val resultat = service.hentSamordningOgInstitusjon(mapOf(1 to listOf(
            lagSaksopplysning(
                kode = AnnenYtelse.SAKSOPPLYSNINGKODE,
                attributter = listOf(
                    lagAttributt(AnnenYtelse.ATTRIBUTT_TYPE, AnnenYtelseType.UFORETRYGD.kode),
                    lagAttributt(AnnenYtelse.ATTRIBUTT_BELOP_PERIODE, BelopPeriode.MND.kode),
                    lagAttributt(AnnenYtelse.ATTRIBUTT_GRAD, "50"),
                    lagAttributt(AnnenYtelse.ATTRIBUTT_BELOP, "0"),
                ),
            ),
        )))

        val ytelse = resultat[1]!!.andreYtelser.single()
        assertThat(resultat[1]!!.institusjonOpphold).isEmpty()
        assertThat(ytelse.type).isEqualTo(AnnenYtelseType.UFORETRYGD)
        assertThat(ytelse.belopPeriode).isEqualTo(BelopPeriode.MND)
        assertThat(ytelse.grad).isEqualTo("50")
        assertThat(ytelse.beløp).isEqualTo("0")
    }

    @Test
    fun `hentSamordningOgInstitusjon utelater AAOKYT nar TYPE-attributt mangler`() {
        val resultat = service.hentSamordningOgInstitusjon(mapOf(1 to listOf(
            lagSaksopplysning(kode = AnnenYtelse.SAKSOPPLYSNINGKODE, attributter = emptyList()),
        )))

        assertThat(resultat[1]?.andreYtelser).isEmpty()
    }

    @Test
    fun `hentSamordningOgInstitusjon mapper INSOPPH til InstitusjonOpphold for helseinstitusjon`() {
        // INSTA=J aktiverer helseinstitusjon; datoformat fra Arena er dd-MM-yyyy
        val resultat = service.hentSamordningOgInstitusjon(mapOf(1 to listOf(
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
        )))

        val opphold = resultat[1]!!.institusjonOpphold.single()
        assertThat(resultat[1]!!.andreYtelser).isEmpty()
        assertThat(opphold.type).isEqualTo(InstitusjonOppholdType.HELSEINSTITUSJON)
        assertThat(opphold.fra).isEqualTo(LocalDate.of(2024, 1, 1))
        assertThat(opphold.til).isEqualTo(LocalDate.of(2024, 12, 31))
        assertThat(opphold.friKostOgLosji).isTrue()
        assertThat(opphold.reduksjonsType).isEqualTo(ReduksjonType.INGEN)
    }

    @Test
    fun `hentSamordningOgInstitusjon mapper INSOPPH til InstitusjonOpphold for straffegjennomforing`() {
        // STRFG=J identifiserer straffegjennomføring
        val resultat = service.hentSamordningOgInstitusjon(mapOf(1 to listOf(
            lagSaksopplysning(
                kode = InstitusjonOpphold.SAKSOPPLYSNINGKODE,
                attributter = listOf(
                    lagAttributt(InstitusjonOpphold.ATTRIBUTT_STRAFFEGJENNOMFORING, "J"),
                    lagAttributt(InstitusjonOpphold.ATTRIBUTT_FRA, "01-03-2023"),
                ),
            ),
        )))

        val opphold = resultat[1]!!.institusjonOpphold.single()
        assertThat(opphold.type).isEqualTo(InstitusjonOppholdType.FENGSEL)
        assertThat(opphold.fra).isEqualTo(LocalDate.of(2023, 3, 1))
        assertThat(opphold.til).isNull()
        assertThat(opphold.friKostOgLosji).isFalse()
    }

    @Test
    fun `hentSamordningOgInstitusjon utelater INSOPPH nar bade STRFG og INSTA er N`() {
        val resultat = service.hentSamordningOgInstitusjon(mapOf(1 to listOf(
            lagSaksopplysning(
                kode = InstitusjonOpphold.SAKSOPPLYSNINGKODE,
                attributter = listOf(
                    lagAttributt(InstitusjonOpphold.ATTRIBUTT_STRAFFEGJENNOMFORING, "N"),
                    lagAttributt(InstitusjonOpphold.ATTRIBUTT_INSTA, "N"),
                    lagAttributt(InstitusjonOpphold.ATTRIBUTT_FRA, "01-01-2024"),
                ),
            ),
        )))

        assertThat(resultat[1]?.institusjonOpphold).isEmpty()
    }

    @Test
    fun `hentSamordningOgInstitusjon utelater InstitusjonOpphold uten fra-dato`() {
        val resultat = service.hentSamordningOgInstitusjon(mapOf(1 to listOf(
            lagSaksopplysning(
                kode = InstitusjonOpphold.SAKSOPPLYSNINGKODE,
                attributter = listOf(lagAttributt(InstitusjonOpphold.ATTRIBUTT_INSTA, "J")),
            ),
        )))

        assertThat(resultat[1]?.institusjonOpphold).isEmpty()
    }

    @Test
    fun `hentSamordningOgInstitusjon holder resultater adskilt per vedtakId`() {
        // Hvert vedtak skal ha sin egen SamordningOgInstitusjon — ikke blandes sammen
        val resultat = service.hentSamordningOgInstitusjon(mapOf(
            1 to listOf(lagSaksopplysning(
                kode = InstitusjonOpphold.SAKSOPPLYSNINGKODE,
                attributter = listOf(
                    lagAttributt(InstitusjonOpphold.ATTRIBUTT_STRAFFEGJENNOMFORING, "J"),
                    lagAttributt(InstitusjonOpphold.ATTRIBUTT_FRA, "01-01-2024"),
                ),
            )),
            2 to listOf(lagSaksopplysning(
                kode = AnnenYtelse.SAKSOPPLYSNINGKODE,
                attributter = listOf(lagAttributt(AnnenYtelse.ATTRIBUTT_TYPE, AnnenYtelseType.UFORETRYGD.kode)),
            )),
        ))

        assertThat(resultat[1]!!.institusjonOpphold).hasSize(1)
        assertThat(resultat[1]!!.andreYtelser).isEmpty()
        assertThat(resultat[2]!!.andreYtelser).hasSize(1)
        assertThat(resultat[2]!!.institusjonOpphold).isEmpty()
    }

    private fun lagSaksopplysning(
        kode: String,
        id: Long = 1L,
        verdi: String? = null,
        attributter: List<ArenaSaksopplysningAttributt> = emptyList(),
    ) = ArenaSaksopplysning(
        saksopplysningId = id,
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
