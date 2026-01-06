package arenaoppslag.intern

import arenaoppslag.datasource.map
import arenaoppslag.modeller.AnnenReduksjon
import arenaoppslag.modeller.Maksimum
import arenaoppslag.modeller.Reduksjon
import arenaoppslag.modeller.UtbetalingMedMer
import arenaoppslag.modeller.Vedtak
import arenaoppslag.modeller.VedtaksType
import no.nav.aap.arenaoppslag.kontrakt.intern.ArenaSak
import no.nav.aap.arenaoppslag.kontrakt.intern.SakStatus
import no.nav.aap.arenaoppslag.kontrakt.intern.Status
import org.intellij.lang.annotations.Language
import org.jetbrains.annotations.TestOnly
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.Date
import java.sql.ResultSet
import java.time.LocalDate
import no.nav.aap.arenaoppslag.kontrakt.modeller.Periode as KontraktPeriode

private val log = LoggerFactory.getLogger(InternDao::class.java)

// Tabellene i Arena er beskrevet her:
// https://confluence.adeo.no/spaces/ARENA/pages/122716553/Arena+-+Datamodell
// Vi bruker views av disse tabellene for AAP, definert her.
// Disse har samme felt som tabellene.
// https://confluence.adeo.no/spaces/TEAMARENA/pages/553617512/ARENA-8716+03+-+L%C3%B8sningsbeskrivelse#ARENA871603L%C3%B8sningsbeskrivelse-Arbeidsavklaringspenger

object InternDao {
    @Language("OracleSql")
    private val selectMaksimumMedTidsbegrensning = """
        SELECT vedtak_id, til_dato, fra_dato, vedtaktypekode, vedtakstatuskode, sak_id, aktfasekode 
          FROM vedtak 
         WHERE person_id = 
               (SELECT person_id 
                  FROM person 
                 WHERE fodselsnr = ?) 
           AND utfallkode = 'JA' 
           AND rettighetkode = 'AAP'
           AND vedtaktypekode IN ('O', 'E', 'G', 'S')
           AND vedtakstatuskode IN ('IVERK', 'AVSLU')
           AND (fra_dato <= til_dato OR til_dato IS NULL)
           AND (til_dato >= ? OR til_dato IS NULL) 
           AND fra_dato <= ?
    """.trimIndent()

    @Language("OracleSql")
    private val selectSaksIdByFnr = """
        SELECT vedtakstatuskode, sak_id, fra_dato, til_dato
          FROM vedtak
         WHERE person_id = 
               (SELECT person_id 
                  FROM person 
                 WHERE fodselsnr = ?) 
           AND rettighetkode = 'AAP'
           AND vedtaktypekode IN ('O', 'E', 'G', 'S')
           AND (fra_dato <= til_dato OR til_dato IS NULL)
    """.trimIndent()

    @Language("OracleSql")
    private val selectAlleSakerByFnr = """
        SELECT vedtakstatuskode, vedtaktypekode, sak_id, fra_dato, til_dato, rettighetkode
          FROM vedtak
         WHERE person_id = 
               (SELECT person_id 
                  FROM person 
                 WHERE fodselsnr = ?) 
    """.trimIndent()

    @Language("OracleSql")
    private val hentBeregningsgrunnlag = """
        SELECT vedtakfaktakode, vedtakverdi
            FROM vedtakfakta 
             WHERE vedtak_id = ? AND vedtakfaktakode IN ('GRUNN')
    """.trimIndent()

    @Language("OracleSql")
    private val hentVedtakfakta = """
        SELECT vedtakfaktakode, vedtakverdi
            FROM vedtakfakta 
             WHERE vedtak_id = ? AND vedtakfaktakode IN ('DAGSMBT', 'BARNTILL', 'DAGS', 'BARNMSTON', 'DAGSFSAM')
    """.trimIndent()

    @Language("OracleSql")
    private val selectVedtakMedTidsbegrensningSql = """
        SELECT til_dato, fra_dato
          FROM vedtak 
         WHERE person_id = 
               (SELECT person_id 
                  FROM person 
                 WHERE fodselsnr = ?) 
           AND utfallkode = 'JA' 
           AND rettighetkode = 'AAP'
           AND vedtaktypekode IN ('O', 'E', 'G')
           AND vedtakstatuskode IN ('IVERK', 'AVSLU')
           AND (fra_dato <= til_dato OR til_dato IS NULL)
           AND (til_dato >= ? OR til_dato IS NULL) 
           AND fra_dato <= ?
    """.trimIndent()

    @Language("OracleSql")
    private val selectSykedagerMeldekort = """
        SELECT sum(verdi)
          FROM anmerkning
        WHERE tabellnavnalias = 'MKORT'
          AND objekt_id       = ?
          AND anmerkningkode  = 'FSNN'
    """.trimIndent()

    @Language("OracleSql")
    private val selectForSentMeldekort = """
        SELECT sum(verdi)
          FROM anmerkning
        WHERE tabellnavnalias = 'MKORT'
          AND objekt_id       = ?
          AND anmerkningkode  = 'SENN'
    """.trimIndent()

    @Language("OracleSql")
    private val selectFraværMeldekort = """
        SELECT sum(verdi)
          FROM anmerkning
        WHERE tabellnavnalias = 'MKORT'
          AND objekt_id       = ?
          AND anmerkningkode  = 'FXNN'
    """.trimIndent()
    //Syk=FSNN', fravære = 'FXNN' og for sent = 'SENN'

    @Language("OracleSql")
    //henter timer arbeidet for bruker x mellom y og z dato gruppert på meldekortperiode
    private val selectTimerArbeidetIMeldekortPeriode = """
        SELECT 
            SUM(mkd.timer_arbeidet) AS timer_arbeidet,
            p.meldekort_id,
            p.belop,
            p.dato_periode_fra,
            p.dato_periode_til
        FROM 
            meldekort m
        JOIN 
            meldekortdag mkd ON mkd.meldekort_id = m.meldekort_id
        JOIN 
            (SELECT dato_periode_fra, dato_periode_til, belop, meldekort_id
             FROM postering
             WHERE vedtak_id = ?) p
            ON m.meldekort_id = p.meldekort_id
        WHERE 
            m.person_id = (SELECT person_id FROM person WHERE fodselsnr = ?)
        AND 
            p.dato_periode_til >= ? AND p.dato_periode_fra <= ?
        GROUP BY
            p.meldekort_id, p.dato_periode_fra, p.dato_periode_til, p.belop
    """.trimIndent()

    @Language("OracleSql")
    private val selectVedtakMedTidsbegrensningMed11_17Sql = """
        SELECT v.til_dato, v.fra_dato, af.aktfasenavn, v.aktfasekode
          FROM vedtak v
          JOIN aktivitetfase af ON v.aktfasekode = af.aktfasekode
         WHERE person_id = 
               (SELECT person_id 
                  FROM person 
                 WHERE fodselsnr = ?) 
           AND utfallkode = 'JA' 
           AND rettighetkode = 'AAP'
           AND vedtaktypekode IN ('O', 'E', 'G')
           AND vedtakstatuskode IN ('IVERK', 'AVSLU')
           AND (fra_dato <= til_dato OR til_dato IS NULL)
           AND (til_dato >= ? OR til_dato IS NULL) 
           AND fra_dato <= ?
    """.trimIndent()

    @Language("OracleSql")
    private val selectPersonMedFnrEksisterer = """
        SELECT * 
        FROM person 
        WHERE fodselsnr = ?
    """.trimIndent()

    fun selectPersonMedFnrEksisterer(
        fodselsnr: String,
        connection: Connection
    ): Boolean {
        return connection.prepareStatement(selectPersonMedFnrEksisterer)
            .use { preparedStatement ->
                preparedStatement.setString(1, fodselsnr)
                val resultSet = preparedStatement.executeQuery()
                resultSet.next()
            }
    }

    fun selectVedtakMedTidsbegrensningOg11_17(
        personId: String,
        fraOgMedDato: LocalDate,
        tilOgMedDato: LocalDate,
        connection: Connection
    ): List<PeriodeMed11_17> {
        return connection.prepareStatement(selectVedtakMedTidsbegrensningMed11_17Sql)
            .use { preparedStatement ->
                preparedStatement.setString(1, personId)
                preparedStatement.setDate(2, Date.valueOf(fraOgMedDato))
                preparedStatement.setDate(3, Date.valueOf(tilOgMedDato))

                val resultSet = preparedStatement.executeQuery()

                val perioder = resultSet.map { row ->
                    PeriodeMed11_17(
                        Periode(
                            fraOgMedDato = row.getDate("fra_dato").toLocalDate(),
                            tilOgMedDato = fraDato(row.getDate("til_dato")),
                        ),
                        aktivitetsfaseKode = row.getString("aktfasekode"),
                        aktivitetsfaseNavn = row.getString("aktfasenavn"),
                    )
                }
                perioder
            }
    }

    fun selectSykedagerMeldekort(meldekortId: String, connection: Connection): Int {
        return connection.prepareStatement(selectSykedagerMeldekort)
            .use { preparedStatement ->
                preparedStatement.setString(1, meldekortId)

                val resultSet = preparedStatement.executeQuery()

                resultSet.map { row ->
                    row.getInt(1)
                }.firstOrNull() ?: 0
            }
    }

    fun selectFraværMeldekort(meldekortId: String, connection: Connection): Int {
        return connection.prepareStatement(selectFraværMeldekort)
            .use { preparedStatement ->
                preparedStatement.setString(1, meldekortId)

                val resultSet = preparedStatement.executeQuery()

                resultSet.map { row ->
                    row.getInt(1)
                }.firstOrNull() ?: 0
            }
    }

    fun selectForSentMeldekort(meldekortId: String, connection: Connection): Boolean {
        return connection.prepareStatement(selectForSentMeldekort)
            .use { preparedStatement ->
                preparedStatement.setString(1, meldekortId)

                val resultSet = preparedStatement.executeQuery()

                val forSent = resultSet.map { row ->
                    row.getInt(1)
                }.firstOrNull() ?: 0
                forSent > 0
            }
    }

    fun selectVedtakPerioder(
        fodselsnr: String,
        fraOgMedDato: LocalDate,
        tilOgMedDato: LocalDate,
        connection: Connection
    ): List<Periode> {
        return connection.prepareStatement(selectVedtakMedTidsbegrensningSql)
            .use { preparedStatement ->
                preparedStatement.setString(1, fodselsnr)
                preparedStatement.setDate(2, Date.valueOf(fraOgMedDato))
                preparedStatement.setDate(3, Date.valueOf(tilOgMedDato))

                val resultSet = preparedStatement.executeQuery()

                val perioder = resultSet.map { row ->
                    Periode(
                        fraOgMedDato = row.getDate("fra_dato").toLocalDate(),
                        tilOgMedDato = fraDato(row.getDate("til_dato")),
                    )
                }.toList()

                perioder
            }
    }

    fun selectUtbetalingVedVedtakId(
        vedtakId: Int,
        connection: Connection,
        barnetiTillegg: Int,
        dagsats: Int,
        personId: String,
        fraDato: LocalDate,
        tilDato: LocalDate
    ): List<UtbetalingMedMer> {
        return connection.prepareStatement(selectTimerArbeidetIMeldekortPeriode)
            .use { preparedStatement ->
                preparedStatement.setInt(1, vedtakId)
                preparedStatement.setString(2, personId)
                preparedStatement.setDate(3, Date.valueOf(fraDato))
                preparedStatement.setDate(4, Date.valueOf(tilDato))

                val resultSet = preparedStatement.executeQuery()

                resultSet.map { row ->
                    //hent andmerking for sent meldekort
                    val meldekortId = row.getString("meldekort_id")
                    UtbetalingMedMer(
                        reduksjon = Reduksjon(
                            timerArbeidet = row.getFloat("timer_arbeidet").toDouble(),
                            annenReduksjon = AnnenReduksjon(
                                selectSykedagerMeldekort(meldekortId, connection).toFloat(),
                                selectForSentMeldekort(meldekortId, connection),
                                selectFraværMeldekort(meldekortId, connection).toFloat()
                            )
                        ),
                        periode = Periode(
                            fraOgMedDato = row.getDate("dato_periode_fra").toLocalDate(),
                            tilOgMedDato = row.getDate("dato_periode_til").toLocalDate(),
                        ),
                        belop = row.getInt("belop"),
                        dagsats = dagsats,
                        barnetillegg = barnetiTillegg
                    )
                }.toList()
            }
    }

    fun selectBeregningsgrunnlag(vedtakId: Int, connection: Connection): Int {
        log.info("Henter beregningsgrunnlag for vedtak $vedtakId.")
        return connection.prepareStatement(hentBeregningsgrunnlag)
            .use { preparedStatement ->
                preparedStatement.setInt(1, vedtakId)
                val resultSet = preparedStatement.executeQuery()
                var beregningsgrunnlag: Int? = null
                resultSet.map { row ->
                    if (row.getString("vedtakfaktakode") == "GRUNN") {
                        beregningsgrunnlag = row.getInt("vedtakverdi")
                    }
                }
                return@use beregningsgrunnlag ?: 0
            }
    }

    fun selectVedtakFakta(vedtakId: Int, connection: Connection): VedtakFakta {
        return connection.prepareStatement(hentVedtakfakta)
            .use { preparedStatement ->
                preparedStatement.setInt(1, vedtakId)
                val resultSet = preparedStatement.executeQuery()
                val vedtakfakta = VedtakFakta(0, 0, 0, 0, 0)
                resultSet.map { row ->
                    when (row.getString("vedtakfaktakode")) {
                        "DAGSMBT" -> vedtakfakta.dagsmbt = row.getInt("vedtakverdi")
                        "BARNTILL" -> vedtakfakta.barntill = row.getInt("vedtakverdi")
                        "DAGS" -> vedtakfakta.dags = row.getInt("vedtakverdi")
                        "BARNMSTON" -> vedtakfakta.barnmston = row.getInt("vedtakverdi")
                        "DAGSFSAM" -> vedtakfakta.dagsfsam = row.getInt("vedtakverdi")
                    }
                }
                vedtakfakta
            }
    }

    fun selectVedtakMaksimum(
        personId: String,
        fraOgMedDato: LocalDate,
        tilOgMedDato: LocalDate,
        connection: Connection
    ): Maksimum {
        log.info("Henter maksimumvedtak for periode $fraOgMedDato - $tilOgMedDato.")
        val maksimum =
            connection.prepareStatement(selectMaksimumMedTidsbegrensning)
                .use { preparedStatement ->
                    preparedStatement.setString(1, personId)
                    preparedStatement.setDate(2, Date.valueOf(fraOgMedDato))
                    preparedStatement.setDate(3, Date.valueOf(tilOgMedDato))

                    val resultSet = preparedStatement.executeQuery()
                    val utbetalinger = mutableListOf<UtbetalingMedMer>()
                    var c = 0;
                    val vedtak = resultSet.map { row ->
                        val vedtakId = row.getInt("vedtak_id")
                        log.info("Henter utbetalinger for vedtak $vedtakId. Iterasjon nr $c.")
                        val vedtakFakta = selectVedtakFakta(vedtakId, connection)
                        utbetalinger.addAll(
                            selectUtbetalingVedVedtakId(
                                connection = connection,
                                barnetiTillegg = vedtakFakta.barntill,
                                dagsats = vedtakFakta.dagsmbt,
                                personId = personId,
                                vedtakId = row.getInt("vedtak_id"),
                                fraDato = fraOgMedDato,
                                tilDato = tilOgMedDato
                            )
                        )
                        val vedtaktypekode = row.getString("vedtaktypekode")
                        c++;
                        Vedtak(
                            vedtaksId = vedtakId.toString(),
                            utbetaling = utbetalinger,
                            dagsats = vedtakFakta.dagsfsam,
                            status = row.getString("vedtakstatuskode"),
                            saksnummer = row.getString("sak_id"),
                            vedtaksdato = row.getString("fra_dato"),
                            rettighetsType = row.getString("aktfasekode"),
                            periode = Periode(
                                fraOgMedDato = row.getDate("fra_dato").toLocalDate(),
                                tilOgMedDato = fraDato(row.getDate("til_dato"))
                            ),
                            beregningsgrunnlag = selectBeregningsgrunnlag(vedtakId, connection),
                            barnMedStonad = vedtakFakta.barnmston,
                            vedtaksTypeKode = vedtaktypekode,
                            vedtaksTypeNavn = VedtaksType.entries
                                .find { it.kode == vedtaktypekode }?.navn
                                ?: error("Ukjent verdi vedtaktypekode=$vedtaktypekode")
                        )
                    }.toList()
                    Maksimum(vedtak)
                }
        return maksimum
    }

    private fun fraDato(date: Date?) = date?.toLocalDate()

    fun selectSaker(personidentifikator: String, connection: Connection): List<SakStatus> {
        connection.prepareStatement(selectSaksIdByFnr)
            .use { preparedStatement ->
                preparedStatement.setString(1, personidentifikator)
                val resultSet = preparedStatement.executeQuery()
                return resultSet.map { row -> mapperForSakStatus(row) }.toList()
            }
    }

    @TestOnly
    internal fun selectAlleSaker(personidentifikator: String, connection: Connection): List<ArenaSak> {
        connection.prepareStatement(selectAlleSakerByFnr)
            .use { preparedStatement ->
                preparedStatement.setString(1, personidentifikator)
                val resultSet = preparedStatement.executeQuery()
                return resultSet.map { row -> mapperForArenasak(row) }.toList()
            }
    }


    private fun mapperForSakStatus(row: ResultSet): SakStatus = SakStatus(
        row.getString("sak_id"),
        Status.entries.find { it.name == row.getString("vedtakstatuskode") }
            ?: Status.UKJENT,
        KontraktPeriode(
            fraOgMedDato = fraDato(row.getDate("fra_dato")),
            tilOgMedDato = fraDato(row.getDate("til_dato"))
        )
    )

    fun mapperForArenasak(row: ResultSet): ArenaSak = ArenaSak(
        row.getString("sak_id"),
        row.getString("vedtakstatuskode"),
        row.getString("vedtaktypekode"),
        fraDato(row.getDate("fra_dato")),
        tilDato = fraDato(row.getDate("til_dato")),
        rettighetkode = row.getString("rettighetkode")
    )

}
