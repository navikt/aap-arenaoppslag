package no.nav.aap.arenaoppslag.service

import io.mockk.mockk
import no.nav.aap.arenaoppslag.database.SaksopplysningRepository
import no.nav.aap.arenaoppslag.modeller.ArenaSaksopplysning
import no.nav.aap.arenaoppslag.modeller.ArenaSaksopplysningAttributt
import no.nav.aap.arenaoppslag.modeller.AnnenYtelse
import no.nav.aap.arenaoppslag.modeller.AnnenYtelseType
import no.nav.aap.arenaoppslag.modeller.Attributtkode
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

        assertThat(resultat[1]?.institusjonOpphold).isNull()
        assertThat(resultat[1]?.andreYtelser).isEmpty()
    }

    @Test
    fun `hentSamordningOgInstitusjon mapper AAOKYT-saksopplysning til AnnenYtelse`() {
        // Arena-eksempel: saksopplysningkode er alltid AAOKYT, type hentes fra TYPE-attributt
        val resultat = service.hentSamordningOgInstitusjon(mapOf(1 to listOf(
            lagSaksopplysning(
                kode = AnnenYtelse.KODE,
                attributter = listOf(
                    lagAttributt(Attributtkode.TYPE, AnnenYtelseType.UFORETRYGD.kode),
                    lagAttributt(Attributtkode.BELPR, BelopPeriode.MND.kode),
                    lagAttributt(Attributtkode.GRAD, "50"),
                    lagAttributt(Attributtkode.BELOP, "0"),
                ),
            ),
        )))

        val ytelse = resultat[1]!!.andreYtelser.single()
        assertThat(resultat[1]!!.institusjonOpphold).isNull()
        assertThat(ytelse.type).isEqualTo(AnnenYtelseType.UFORETRYGD)
        assertThat(ytelse.belopPeriode).isEqualTo(BelopPeriode.MND)
        assertThat(ytelse.grad).isEqualTo("50")
        assertThat(ytelse.beløp).isEqualTo("0")
    }

    @Test
    fun `hentSamordningOgInstitusjon utelater AAOKYT nar TYPE-attributt mangler`() {
        val resultat = service.hentSamordningOgInstitusjon(mapOf(1 to listOf(
            lagSaksopplysning(kode = AnnenYtelse.KODE, attributter = emptyList()),
        )))

        assertThat(resultat[1]?.andreYtelser).isEmpty()
    }

    @Test
    fun `hentSamordningOgInstitusjon mapper INSOPPH til InstitusjonOpphold for helseinstitusjon`() {
        // INSTA=J aktiverer helseinstitusjon; datoformat fra Arena er dd-MM-yyyy
        val resultat = service.hentSamordningOgInstitusjon(mapOf(1 to listOf(
            lagSaksopplysning(
                kode = InstitusjonOpphold.KODE,
                attributter = listOf(
                    lagAttributt(InstitusjonOpphold.Attributt.INSTA.kode, "J"),
                    lagAttributt(InstitusjonOpphold.Attributt.FRA.kode, "01-01-2024"),
                    lagAttributt(InstitusjonOpphold.Attributt.TIL.kode, "31-12-2024"),
                    lagAttributt(InstitusjonOpphold.Attributt.FRI_KOST_LOSJI.kode, "J"),
                    lagAttributt(InstitusjonOpphold.Attributt.REDUKSJON.kode, "RED00"),
                ),
            ),
        )))

        val opphold = resultat[1]!!.institusjonOpphold!!
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
                kode = InstitusjonOpphold.KODE,
                attributter = listOf(
                    lagAttributt(InstitusjonOpphold.Attributt.STRAFFEGJENNOMFORING.kode, "J"),
                    lagAttributt(InstitusjonOpphold.Attributt.FRA.kode, "01-03-2023"),
                ),
            ),
        )))

        val opphold = resultat[1]!!.institusjonOpphold!!
        assertThat(opphold.type).isEqualTo(InstitusjonOppholdType.FENGSEL)
        assertThat(opphold.fra).isEqualTo(LocalDate.of(2023, 3, 1))
        assertThat(opphold.til).isNull()
        assertThat(opphold.friKostOgLosji).isFalse()
    }

    @Test
    fun `hentSamordningOgInstitusjon utelater INSOPPH nar bade STRFG og INSTA er N`() {
        val resultat = service.hentSamordningOgInstitusjon(mapOf(1 to listOf(
            lagSaksopplysning(
                kode = InstitusjonOpphold.KODE,
                attributter = listOf(
                    lagAttributt(InstitusjonOpphold.Attributt.STRAFFEGJENNOMFORING.kode, "N"),
                    lagAttributt(InstitusjonOpphold.Attributt.INSTA.kode, "N"),
                    lagAttributt(InstitusjonOpphold.Attributt.FRA.kode, "01-01-2024"),
                ),
            ),
        )))

        assertThat(resultat[1]?.institusjonOpphold).isNull()
    }

    @Test
    fun `hentSamordningOgInstitusjon utelater InstitusjonOpphold uten fra-dato`() {
        val resultat = service.hentSamordningOgInstitusjon(mapOf(1 to listOf(
            lagSaksopplysning(
                kode = InstitusjonOpphold.KODE,
                attributter = listOf(lagAttributt(InstitusjonOpphold.Attributt.INSTA.kode, "J")),
            ),
        )))

        assertThat(resultat[1]?.institusjonOpphold).isNull()
    }

    @Test
    fun `hentSamordningOgInstitusjon holder resultater adskilt per vedtakId`() {
        // Hvert vedtak skal ha sin egen SamordningOgInstitusjon — ikke blandes sammen
        val resultat = service.hentSamordningOgInstitusjon(mapOf(
            1 to listOf(lagSaksopplysning(
                kode = InstitusjonOpphold.KODE,
                attributter = listOf(
                    lagAttributt(InstitusjonOpphold.Attributt.STRAFFEGJENNOMFORING.kode, "J"),
                    lagAttributt(InstitusjonOpphold.Attributt.FRA.kode, "01-01-2024"),
                ),
            )),
            2 to listOf(lagSaksopplysning(
                kode = AnnenYtelse.KODE,
                attributter = listOf(lagAttributt(Attributtkode.TYPE, AnnenYtelseType.UFORETRYGD.kode)),
            )),
        ))

        assertThat(resultat[1]!!.institusjonOpphold).isNotNull()
        assertThat(resultat[1]!!.andreYtelser).isEmpty()
        assertThat(resultat[2]!!.andreYtelser).hasSize(1)
        assertThat(resultat[2]!!.institusjonOpphold).isNull()
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

    private fun lagAttributt(kode: Attributtkode, verdi: String?) = ArenaSaksopplysningAttributt(
        attributtkode = kode,
        skjermbildetekst = null,
        formatnavn = null,
        posisjon = 1,
        verdi = verdi,
        statusSjekketAv = null,
    )
}
