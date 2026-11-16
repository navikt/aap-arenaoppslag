package no.nav.aap.arenaoppslag.tilgangsmaskin

import no.nav.aap.arenaoppslag.SakOgVedtakService
import no.nav.aap.arenaoppslag.database.SakRepository
import no.nav.aap.arenaoppslag.database.VedtakRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.lang.classfile.ClassFile
import java.lang.classfile.ClassModel
import java.lang.classfile.instruction.InvokeInstruction
import java.nio.file.Files
import java.nio.file.Path

class AutorisasjonArkitekturTest {
    private val pakke = "no/nav/aap/arenaoppslag/"
    private val tilgang = "${pakke}tilgangsmaskin/TilgangkontrollService"
    private val klasser: List<ClassModel> = Files.walk(
        Path.of(TilgangkontrollService::class.java.protectionDomain.codeSource.location.toURI())
    ).use { filer ->
        filer.filter { it.toString().endsWith(".class") }
            .map { ClassFile.of().parse(it) }.toList()
    }

    @Test
    fun `bare tilgangskontrollen kan implementere og opprette autorisasjonsbevis`() {
        val bevis = setOf(
            "${pakke}tilgangsmaskin/AuthorisertPersonId",
            "${pakke}tilgangsmaskin/AutorisertSaksnummer",
        )
        val implementasjoner = klasser.filter { klasse ->
            klasse.interfaces().any { it.asInternalName() in bevis }
        }
        assertThat(implementasjoner.map { it.thisClass().asInternalName() })
            .containsExactlyInAnyOrder("$tilgang\$VerifisertPerson", "$tilgang\$VerifisertSak")

        for (implementasjon in implementasjoner) {
            val navn = implementasjon.thisClass().asInternalName()
            val kall = finnKall(navn, "<init>")
            assertThat(kall).isNotEmpty
            assertThat(kall).allMatch { it.first == tilgang }
        }
    }

    @Test
    fun `beskyttede innganger krever autoriserte typer uten gamle overloads`() {
        fun parametere(klasse: Class<*>, navn: String) = klasse.declaredMethods
            .filter { it.name.substringBefore('$') == navn }
            .map { it.parameterTypes.toList() }

        assertThat(parametere(SakRepository::class.java, "hentSak"))
            .containsExactly(listOf(AutorisertSaksnummer::class.java))
        assertThat(parametere(SakRepository::class.java, "hentSakerDetaljerForPerson"))
            .containsExactly(listOf(AuthorisertPersonId::class.java))
        assertThat(parametere(VedtakRepository::class.java, "hentVedtak").filter {
            it != listOf(String::class.java)
        }).containsExactly(listOf(AuthorisertPersonId::class.java))
        for (navn in listOf("hentVedtakForPerson", "hentVedtakDetaljerForPerson")) {
            assertThat(parametere(SakOgVedtakService::class.java, navn))
                .containsExactly(listOf(AuthorisertPersonId::class.java))
        }
        assertThat(parametere(SakOgVedtakService::class.java, "hentSakMedVedtak"))
            .containsExactly(listOf(AutorisertSaksnummer::class.java))
    }

    @Test
    fun `legacy og raaverdier kan bare brukes fra avtalte kallesteder`() {
        val service = "${pakke}SakOgVedtakService"
        val sakService = "${pakke}service/SakService"
        val tillatte = mapOf(
            ("${pakke}database/SakRepository" to "hentSakUtenTilgangskontroll") to setOf(
                service to "hentSakMedVedtakUtenTilgangskontroll",
                sakService to "hentSak",
                sakService to "hentSakId",
                sakService to "hentPersonForSak",
            ),
            (service to "hentSakMedVedtakUtenTilgangskontroll") to setOf(
                "${pakke}service/HistorikkService" to "hentHistorikkStatusDetalj",
                "${pakke}ApiVisningsklientRouteKt\$sakDetaljert\$1" to "invokeSuspend",
            ),
            ("${pakke}database/VedtakRepository" to "hentVedtakForSak") to setOf(
                service to "getArenaSakMedVedtak",
            ),
            (sakService to "hentPersonForSak") to setOf(
                tilgang to "verifiserTilgangTilSak",
            ),
            (sakService to "hentSak") to setOf(
                "${pakke}MigreringRouteKt" to "hentSakFraSaksnummer",
                "${pakke}ApiVisningsklientRouteKt\$kvotehistorikkForSak\$1" to "invokeSuspend",
                "${pakke}ApiVisningsklientRouteKt\$oppgaverForSak\$1" to "invokeSuspend",
                "${pakke}ApiVisningsklientRouteKt\$telleverkForSak\$1" to "invokeSuspend",
            ),
            (sakService to "hentSakId") to setOf(
                "${pakke}ApiVisningsklientRouteKt\$tilkjentYtelseForSak\$1" to "invokeSuspend",
            ),
        )
        for ((maal, tillatteKallere) in tillatte) {
            val kall = finnKall(maal.first, maal.second)
            assertThat(kall).describedAs("Kall til $maal").isNotEmpty
            assertThat(kall).describedAs("Nye omveier til $maal").isSubsetOf(tillatteKallere)
        }
        val testMetoder = setOf("hentVedtak", "selectVedtak")
        for (klasse in klasser) {
            for (metode in klasse.methods()) {
                metode.code().ifPresent { kode ->
                    kode.filterIsInstance<InvokeInstruction>().forEach { kall ->
                        if (kall.owner().asInternalName().startsWith("${pakke}database/VedtakRepository") &&
                            kall.name().stringValue() in testMetoder &&
                            kall.type().stringValue().startsWith("(Ljava/lang/String;")
                        ) {
                            assertThat(klasse.thisClass().asInternalName())
                                .startsWith("${pakke}database/VedtakRepository")
                        }
                    }
                }
            }
        }
    }

    private fun finnKall(eier: String, navn: String): List<Pair<String, String>> = buildList {
        for (klasse in klasser) {
            for (metode in klasse.methods()) {
                metode.code().ifPresent { kode ->
                    kode.filterIsInstance<InvokeInstruction>().forEach { kall ->
                        if (kall.owner().asInternalName() == eier &&
                            kall.name().stringValue().substringBefore('$') == navn
                        ) {
                            add(klasse.thisClass().asInternalName() to metode.methodName().stringValue().substringBefore('$'))
                        }
                    }
                }
            }
        }
    }
}
