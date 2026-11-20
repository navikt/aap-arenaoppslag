package no.nav.aap.arenaoppslag.database

import no.nav.aap.arenaoppslag.modeller.ArenaVedtak
import no.nav.aap.arenaoppslag.modeller.PersonId
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple.tuple
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.sql.Date
import java.time.LocalDate

class HistorikkRepositoryTest : H2TestBase("flyway/eksisterer") {

    private lateinit var historikkRepository: HistorikkRepository
    private lateinit var vedtakRepository: VedtakRepository
    private val testDato = LocalDate.parse("2025-12-15") // simulerer dagens dato for testene

    @BeforeEach
    fun setUp() {
        historikkRepository = HistorikkRepository(h2)
        vedtakRepository = VedtakRepository(h2)
    }

    @Test
    fun `ingen signifikante vedtak for person som ikke finnes`() {
        val alleVedtak = historikkRepository.hentAlleSignifikanteVedtakForPerson(
            PersonId(54601), /* finnes ikke */
            testDato,
        )
        assertThat(alleVedtak).isEmpty()
    }

    @Test
    fun `ingen signifikante vedtak for person med kun veldig gamle vedtak`() {
        val testPerson = "kun_gamle"
        val testPersonId = PersonId(992)
        val alleVedtak: List<ArenaVedtak> = vedtakRepository.hentVedtak(testPerson)
        assertThat(alleVedtak).hasSize(2)

        val signifikanteVedtak = historikkRepository.hentAlleSignifikanteVedtakForPerson(testPersonId, testDato)
        assertThat(signifikanteVedtak).isEmpty()
    }

    @Test
    fun `finner signifikante vedtak for person med kun nye vedtak`() {
        val testPersonFnr = "kun_nye"
        val testPersonId = PersonId(996)
        val alleVedtak = vedtakRepository.hentVedtak(testPersonFnr)
        assertThat(alleVedtak).hasSize(3) // antall i vedtak-tabellen

        val signifikanteVedtak = historikkRepository.hentAlleSignifikanteVedtakForPerson(testPersonId, testDato)
        assertThat(signifikanteVedtak.map { it.rettighetkode }.sorted()).isEqualTo(
            listOf(
                "AAP"
            )
        )
        assertThat(signifikanteVedtak).allMatch { it.aktivitetsfaseKode == "IKKE" }
        assertThat(signifikanteVedtak).extracting("rettighetkode", "aar", "lopenrvedtak")
            .containsExactlyInAnyOrder(
                tuple("AAP", 2021, 30),
            )
    }

    @Test
    fun `finner signifikante KLAG1 KLAG2 og ANKE vedtak`() {
        val testPersonFnr = "klager_anke"
        val testPersonId = PersonId(998)
        // Sjekk først at vedtakene finnes i databasen og har forventede verdier
        val alleVedtak = vedtakRepository.hentVedtak(testPersonFnr)
        assertThat(alleVedtak).hasSize(3)

        // Sjekk deretter at de regnes som signifikante
        val signifikanteVedtak = historikkRepository.hentAlleSignifikanteVedtakForPerson(testPersonId, testDato)
        assertThat(signifikanteVedtak.map { it.rettighetkode })
            .containsExactlyInAnyOrder("KLAG1", "KLAG2", "ANKE")
    }

    @Test
    fun `finner klager og anker uten INNVF med eller uten andre vedtakfakta`() {
        h2.connection.use { connection ->
            connection.autoCommit = false
            try {
                connection.createStatement().use { statement ->
                    statement.executeUpdate(
                        "DELETE FROM vedtakfakta WHERE vedtak_id IN (998001, 998002, 998003)"
                    )

                    val utenVedtakfakta = HistorikkRepository.hentAlleSignifikanteVedtakForPerson(
                        998, testDato, connection
                    )
                    assertThat(utenVedtakfakta.map { it.vedtakId })
                        .containsExactly(998003, 998002, 998001)

                    statement.executeUpdate(
                        """
                        INSERT INTO vedtakfakta (vedtak_id, vedtakfaktakode, vedtakverdi)
                        VALUES
                            (998001, 'K_INNVF', 'ikke en dato'),
                            (998002, 'K_INNVF', 'ikke en dato'),
                            (998003, 'K_INNVF', 'ikke en dato')
                        """.trimIndent()
                    )

                    val medAndreVedtakfakta = HistorikkRepository.hentAlleSignifikanteVedtakForPerson(
                        998, testDato, connection
                    )
                    assertThat(medAndreVedtakfakta.map { it.vedtakId })
                        .containsExactly(998003, 998002, 998001)
                }
            } finally {
                connection.rollback()
            }
        }
    }

    @Test
    fun `bruker riktige tidsbuffere for klager og anker med INNVF`() {
        val klageTidsbuffer = testDato.minusWeeks(HistorikkRepository.klageTidsbufferUker)
        val ankeTidsbuffer = testDato.minusWeeks(HistorikkRepository.ankeTidsbufferUker)
        val maksimalTidUendret = testDato.minusWeeks(HistorikkRepository.nedreGrenseModifisertSidenKlageAnkeUker)

        h2.connection.use { connection ->
            connection.autoCommit = false
            try {
                connection.createStatement().use { statement ->
                    statement.executeUpdate(
                        """
                        UPDATE vedtak
                        SET utfallkode = 'NEI', mod_dato = DATE '$maksimalTidUendret'
                        WHERE person_id = 998
                        """.trimIndent()
                    )

                    for (dagerEtterTidsbuffer in listOf(0L, -1L)) {
                        statement.executeUpdate(
                            """
                            UPDATE vedtakfakta
                            SET vedtakverdi = TO_CHAR(
                                CASE WHEN vedtak_id = 998003
                                    THEN DATE '${ankeTidsbuffer.plusDays(dagerEtterTidsbuffer)}'
                                    ELSE DATE '${klageTidsbuffer.plusDays(dagerEtterTidsbuffer)}'
                                END,
                                'DD-MM-YYYY'
                            )
                            WHERE vedtak_id IN (998001, 998002, 998003)
                              AND vedtakfaktakode = 'INNVF'
                            """.trimIndent()
                        )

                        val signifikanteVedtak = HistorikkRepository.hentAlleSignifikanteVedtakForPerson(
                            998, testDato, connection
                        )
                        if (dagerEtterTidsbuffer == 0L) {
                            assertThat(signifikanteVedtak.map { it.vedtakId })
                                .containsExactly(998003, 998002, 998001)
                        } else {
                            assertThat(signifikanteVedtak).isEmpty()
                        }
                    }
                }
            } finally {
                connection.rollback()
            }
        }
    }

    @Test
    fun `finner ikke KLAG1 KLAG2 og ANKE eldre enn maksimal behandlingstid`() {
        val testPersonId = PersonId(999)
        // Sjekk først at vedtakene finnes i databasen og har forventede verdier
        assertThat(vedtakRepository.hentVedtak(testPersonId).map { it.rettighetkode })
            .containsExactlyInAnyOrder("KLAG1", "KLAG2", "ANKE")
        // Sjekk at ingen av dem regnes som signifikante
        assertThat(historikkRepository.hentAlleSignifikanteVedtakForPerson(testPersonId, testDato)).isEmpty()
    }

    @Test
    fun `finner ikke KLAG1 KLAG2 og ANKE med utfall TRUKK`() {
        val testPersonId = PersonId(1000)
        val alleVedtak = vedtakRepository.hentVedtak(testPersonId)
        // Sjekk først at vedtakene finnes i databasen og har forventede verdier
        assertThat(alleVedtak.map { it.rettighetkode })
            .containsExactlyInAnyOrder("KLAG1", "KLAG2", "ANKE")
        assertThat(alleVedtak).allMatch { it.utfallkode == "TRUKK" }

        // Sjekk at ingen av dem regnes som signifikante
        assertThat(historikkRepository.hentAlleSignifikanteVedtakForPerson(testPersonId, testDato)).isEmpty()
    }

    @Test
    fun `finner ikke klager behandlet for mer enn 12 uker siden eller anker behandlet for mer enn 52 uker siden`() {
        val testPersonId = PersonId(1001)
        val alleVedtak = vedtakRepository.hentVedtak(testPersonId)
        // Sjekk først at vedtakene finnes i databasen og har forventede verdier
        assertThat(alleVedtak.map { it.rettighetkode })
            .containsExactlyInAnyOrder("KLAG1", "KLAG2", "ANKE")
        assertThat(alleVedtak).allMatch { it.utfallkode == "NEI" }

        // Sjekk at ingen av dem regnes som signifikante
        assertThat(historikkRepository.hentAlleSignifikanteVedtakForPerson(testPersonId, testDato)).isEmpty()
    }

    @Test
    fun `finner signifikante vedtak for person med både gamle og nye vedtak`() {
        val testPersonFnr = "blanding"
        val testPersonId = PersonId(997)
        val alleVedtak = vedtakRepository.hentVedtak(testPersonFnr)
        assertThat(alleVedtak).hasSize(6)

        val signifikanteVedtak = historikkRepository.hentAlleSignifikanteVedtakForPerson(testPersonId, testDato)
        assertThat(signifikanteVedtak).hasSize(1)
    }

    @Test
    fun `Ingen signifikante vedtak for person med kun avslag på AA115`() {
        val testPersonFnr = "426282"
        val testPersonId = PersonId(426282)
        val alleVedtak = vedtakRepository.hentVedtak(testPersonFnr)
        assertThat(alleVedtak).hasSize(1)

        val signifikanteVedtak = historikkRepository.hentAlleSignifikanteVedtakForPerson(testPersonId, testDato)
        assertThat(signifikanteVedtak).hasSize(0)
    }

    @Test
    fun `oppdelte AAP-spørringer bevarer resultatet uten overlapp`() {
        // Testdataene bygger på observasjoner i AAP-uttrekket og forutsetninger for sammenligningen:
        // - GODKJ og KONT forekommer ikke i databasen lenger for rettighetkode AAP.
        // - Vedtak av type S kan ikke ha status MOTAT.
        // - For type O/E/G har IVERK med utfall JA både fra_dato og til_dato.
        //   Andre vedtakstyper kan mangle til_dato.
        // - INNST har alltid fra_dato i uttrekket. til_dato kan mangle.
        // - For type O/E/G har AVSLU med utfall JA enten begge datoene eller ingen av dem.
        // - AVSLU/JA uten datoer er etterregistrerte vedtak og skal ikke tas med i resultatet.
        // - For type O/E/G forekommer ikke IVERK/AVSLU med null utfall og minst én dato.
        // Dette er forutsetninger for testen, ikke garantier fra databaseskjemaet.
        val vanligTidsbuffer = testDato.minusWeeks(HistorikkRepository.vanligTidsbufferUker)
        val maksimalTidUbehandlet = testDato.minusWeeks(HistorikkRepository.maksimalTidUbehandletAAPUker)
        val maksimalTidUendret = testDato.minusWeeks(HistorikkRepository.nedreGrenseModifisertSidenAAPUker)

        h2.connection.use { connection ->
            connection.autoCommit = false
            try {
                connection.createStatement().use { statement ->
                    statement.executeUpdate(
                        """
                        INSERT INTO vedtak (
                            vedtak_id, sak_id, vedtakstatuskode, vedtaktypekode, utfallkode,
                            rettighetkode, person_id, fra_dato, til_dato, aetatenhet_behandler,
                            lopenrsak, aar, lopenrvedtak, aktfasekode, dato_mottatt, mod_dato
                        )
                        SELECT
                            2000000 + ROW_NUMBER() OVER (), 13, status.kode, type.kode, utfall.kode,
                            'AAP', 992, periode.fra_dato, periode.til_dato, '4402',
                            13, 2021, 2000000 + ROW_NUMBER() OVER (), 'IKKE', DATE '$testDato', endret.dato
                        FROM (VALUES (CAST(NULL AS VARCHAR(10))), ('JA'), ('NEI'), ('AVBRUTT')) utfall(kode)
                        CROSS JOIN (VALUES ('OPPRE'), ('MOTAT'), ('REGIS'), ('INNST'), ('IVERK'), ('AVSLU')) status(kode)
                        CROSS JOIN (VALUES ('O'), ('E'), ('G'), ('N')) type(kode)
                        CROSS JOIN (VALUES
                            (CAST(NULL AS DATE), CAST(NULL AS DATE)),
                            (DATE '$testDato', NULL),
                            (NULL, DATE '$testDato'),
                            (DATE '$testDato', DATE '$testDato'),
                            (DATE '${testDato.plusDays(1)}', DATE '$testDato'),
                            (DATE '${vanligTidsbuffer.minusDays(1)}', DATE '${vanligTidsbuffer.minusDays(1)}'),
                            (DATE '$vanligTidsbuffer', DATE '$vanligTidsbuffer'),
                            (DATE '$vanligTidsbuffer', DATE '${vanligTidsbuffer.plusDays(1)}')
                        ) periode(fra_dato, til_dato)
                        CROSS JOIN (VALUES
                            (CAST(NULL AS DATE)),
                            (DATE '$testDato'),
                            (DATE '${maksimalTidUbehandlet.minusDays(1)}'),
                            (DATE '$maksimalTidUbehandlet'),
                            (DATE '${maksimalTidUbehandlet.plusDays(1)}'),
                            (DATE '${maksimalTidUendret.minusDays(1)}'),
                            (DATE '$maksimalTidUendret'),
                            (DATE '${maksimalTidUendret.plusDays(1)}')
                        ) endret(dato)
                        WHERE (status.kode NOT IN ('OPPRE', 'MOTAT') OR utfall.kode IS NULL)
                          AND NOT (
                              type.kode IN ('O', 'E', 'G')
                              AND status.kode IN ('IVERK', 'AVSLU')
                              AND utfall.kode IS NULL
                              AND (periode.fra_dato IS NOT NULL OR periode.til_dato IS NOT NULL)
                          )
                          AND (
                              utfall.kode IS NULL
                              OR utfall.kode <> 'JA'
                              OR type.kode NOT IN ('O', 'E', 'G')
                              OR status.kode NOT IN ('IVERK', 'AVSLU')
                              OR (periode.fra_dato IS NOT NULL AND periode.til_dato IS NOT NULL)
                              OR (status.kode = 'AVSLU' AND periode.fra_dato IS NULL AND periode.til_dato IS NULL)
                          )
                          AND (status.kode <> 'INNST' OR periode.fra_dato IS NOT NULL)
                        """.trimIndent()
                    )
                }

                val opprinneligSpørring = """
                    SELECT v.*
                    FROM vedtak v
                    WHERE v.person_id = ?
                      AND (v.utfallkode IS NULL OR v.utfallkode = 'JA')
                      AND v.rettighetkode = 'AAP'
                      AND NOT (fra_dato > til_dato AND (til_dato IS NOT NULL AND fra_dato IS NOT NULL))
                      AND ((fra_dato IS NOT NULL OR til_dato IS NOT NULL) OR vedtakstatuskode IN ('OPPRE', 'MOTAT', 'REGIS', 'INNST'))
                      AND vedtaktypekode IN ('O','E','G')
                      AND (til_dato IS NULL OR til_dato >= ?)
                      AND NOT (v.utfallkode IS NULL AND v.mod_dato <= ?)
                      AND v.MOD_DATO >= ?
                    ORDER BY aar DESC, lopenrsak DESC, lopenrvedtak DESC
                """.trimIndent()

                val forventedeVedtak = connection.createParameterizedQuery(opprinneligSpørring).use { statement ->
                    statement.setInt(1, 992)
                    statement.setDate(2, Date.valueOf(vanligTidsbuffer))
                    statement.setDate(3, Date.valueOf(maksimalTidUbehandlet))
                    statement.setDate(4, Date.valueOf(maksimalTidUendret))
                    statement.executeQuery().use { resultSet ->
                        resultSet.map { HistorikkRepository.mapperForArenaVedtak(it) }
                    }
                }
                val innvilgedeVedtak = connection.createParameterizedQuery(
                    HistorikkRepository.selectKunRelevanteAapVedtak
                ).use { statement ->
                    statement.setInt(1, 992)
                    statement.setDate(2, Date.valueOf(vanligTidsbuffer))
                    statement.setDate(3, Date.valueOf(maksimalTidUendret))
                    statement.executeQuery().use { resultSet ->
                        resultSet.map { HistorikkRepository.mapperForArenaVedtak(it) }
                    }
                }
                val vedtakUnderBehandling = connection.createParameterizedQuery(
                    HistorikkRepository.selectKunRelevanteAapVedtakUnderBehandling
                ).use { statement ->
                    statement.setInt(1, 992)
                    statement.setDate(2, Date.valueOf(vanligTidsbuffer))
                    statement.setDate(3, Date.valueOf(maksimalTidUbehandlet))
                    statement.setDate(4, Date.valueOf(maksimalTidUendret))
                    statement.executeQuery().use { resultSet ->
                        resultSet.map { HistorikkRepository.mapperForArenaVedtak(it) }
                    }
                }
                val signifikanteVedtak = HistorikkRepository.hentAlleSignifikanteVedtakForPerson(
                    992, testDato, connection
                )

                assertThat(innvilgedeVedtak).isNotEmpty().containsExactlyInAnyOrderElementsOf(
                    forventedeVedtak.filter {
                        it.utfallkode == "JA" && it.statusKode in setOf("IVERK", "AVSLU")
                                && it.fraOgMed != null && it.tilDato != null
                    }
                )
                assertThat(vedtakUnderBehandling).isNotEmpty().containsExactlyInAnyOrderElementsOf(
                    forventedeVedtak.filter {
                        it.utfallkode == null || it.statusKode !in setOf("IVERK", "AVSLU")
                                || it.fraOgMed == null || it.tilDato == null
                    }
                )
                assertThat(vedtakUnderBehandling.filter {
                    it.utfallkode == "JA" && it.fraOgMed == null && it.tilDato == null
                })
                    .extracting("statusKode", "fraOgMed", "tilDato")
                    .containsOnly(tuple("REGIS", null, null))
                assertThat(vedtakUnderBehandling.filter { it.statusKode == "INNST" })
                    .isNotEmpty()
                    .allMatch { it.fraOgMed != null }
                    .anyMatch { it.tilDato == null }
                assertThat(vedtakUnderBehandling.filter { it.statusKode in setOf("OPPRE", "MOTAT") })
                    .extracting("statusKode", "utfallkode")
                    .containsOnly(tuple("OPPRE", null), tuple("MOTAT", null))
                assertThat(signifikanteVedtak).containsExactlyElementsOf(forventedeVedtak)
            } finally {
                connection.rollback()
            }
        }
    }

}
