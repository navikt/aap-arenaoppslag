package no.nav.aap.arenaoppslag.database

import no.nav.aap.arenaoppslag.database.DbDato.fraDato
import no.nav.aap.arenaoppslag.modeller.AnnenReduksjon
import no.nav.aap.arenaoppslag.modeller.Maksimum
import no.nav.aap.arenaoppslag.modeller.Periode
import no.nav.aap.arenaoppslag.modeller.Reduksjon
import no.nav.aap.arenaoppslag.modeller.UtbetalingMedMer
import no.nav.aap.arenaoppslag.modeller.Vedtak
import no.nav.aap.arenaoppslag.modeller.VedtakFakta
import no.nav.aap.arenaoppslag.modeller.VedtaksType
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.Date
import java.time.LocalDate
import javax.sql.DataSource

class MaksimumRepository(
    private val dataSource: DataSource,
    private val chunkStørrelse: Int = 999,
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    fun hentMaksimumsløsning(
        fodselsnr: String,
        fraOgMedDato: LocalDate,
        tilOgMedDato: LocalDate,
    ): Maksimum =
        dataSource.connection.use { con ->
            selectVedtakMaksimum(fodselsnr, fraOgMedDato, tilOgMedDato, con)
        }


    private fun selectVedtakMaksimum(
        fodselsnr: String,
        fraOgMedDato: LocalDate,
        tilOgMedDato: LocalDate,
        connection: Connection,
    ): Maksimum {
        log.info("Henter maksimumvedtak for periode $fraOgMedDato - $tilOgMedDato.")
        return connection.prepareStatement(selectMaksimumMedTidsbegrensning).use { preparedStatement ->
            preparedStatement.setString(1, fodselsnr)
            preparedStatement.setDate(2, Date.valueOf(fraOgMedDato))
            preparedStatement.setDate(3, Date.valueOf(tilOgMedDato))

            val resultSet = preparedStatement.executeQuery()
            var c = 0
            val meldekortrader = hentUtbetalingerForPerson(
                connection = connection,
                fodselsnr = fodselsnr,
            )

            val anmerkningerPerMeldekort =
                selectAlleMeldekortAnmerkninger(meldekortrader.map { rad -> rad.meldekortId }.distinct(), connection)

            log.info("Fant ${anmerkningerPerMeldekort.size} meldekort. Fra-dato: $fraOgMedDato, til-dato: $tilOgMedDato.")

            val vedtak = resultSet.map { row ->
                val vedtakId = row.getInt("vedtak_id")
                log.info("Henter utbetalinger for vedtak $vedtakId. Iterasjon nr $c.")
                val vedtakFakta = selectVedtakFakta(vedtakId, connection)

                val vedtaksperiode = Periode(
                    fraOgMedDato = row.getDate("fra_dato").toLocalDate(),
                    tilOgMedDato = fraDato(row.getDate("til_dato")),
                )

                val utbetalinger = utbetalingerForVedtak(meldekortrader, vedtaksperiode).map { rad ->
                    mapTilUtbetaling(
                        rad,
                        anmerkningerPerMeldekort,
                        vedtakFakta.dagsmbt,
                        vedtakFakta.barntill
                    )
                }

                val vedtaktypekode = row.getString("vedtaktypekode")
                c++
                Vedtak(
                    vedtaksId = vedtakId.toString(),
                    utbetaling = utbetalinger,
                    dagsats = vedtakFakta.dagsfsam,
                    status = row.getString("vedtakstatuskode"),
                    saksnummer = row.getString("sak_id"),
                    vedtaksdato = row.getString("fra_dato"),
                    rettighetsType = row.getString("aktfasekode"),
                    periode = vedtaksperiode,
                    beregningsgrunnlag = selectBeregningsgrunnlag(vedtakId, connection),
                    barnetillegg = vedtakFakta.barntill,
                    barnMedStonad = vedtakFakta.barnmston,
                    justertG = vedtakFakta.justertg,
                    vedtaksTypeKode = vedtaktypekode,
                    vedtaksTypeNavn = VedtaksType.entries.find { it.kode == vedtaktypekode }?.navn
                        ?: error("Ukjent verdi vedtaktypekode=$vedtaktypekode"),
                    lopenrvedtak = row.getInt("lopenrvedtak"),
                    relatertVedtak = row.getIntOrNull("vedtak_id_relatert"),
                    utfallkode = row.getString("utfallkode")
                )
            }
            Maksimum(vedtak)
        }
    }

    private fun utbetalingerForVedtak(
        meldekortrader: List<MeldekortRad>,
        vedtaksperiode: Periode,
    ): List<MeldekortRad> {
        // G-regulering kan ugyldiggjøre vedtaket posteringen peker på, uten at utbetalingen bortfaller.
        return meldekortrader.filter { rad ->
            (vedtaksperiode.fraOgMedDato == null ||
                    rad.posteringsperiodeFra >= vedtaksperiode.fraOgMedDato) &&
                    (vedtaksperiode.tilOgMedDato == null ||
                            rad.posteringsperiodeTil <= vedtaksperiode.tilOgMedDato)
        }
    }

    private fun hentUtbetalingerForPerson(
        connection: Connection,
        fodselsnr: String,
    ): List<MeldekortRad> {
        return connection.prepareStatement(selectUtbetalingerForPerson).use { preparedStatement ->
            preparedStatement.setString(1, fodselsnr)
            preparedStatement.setString(2, fodselsnr)

            preparedStatement.executeQuery().map { row ->
                MeldekortRad(
                    meldekortId = row.getLong("meldekort_id"),
                    timerArbeidet = row.getFloat("timer_arbeidet").toDouble(),
                    posteringsperiodeFra = row.getDate("dato_periode_fra").toLocalDate(),
                    posteringsperiodeTil = row.getDate("dato_periode_til").toLocalDate(),
                    belop = row.getInt("belop"),
                )
            }
        }
    }

    private fun selectAlleMeldekortAnmerkninger(
        meldekortIder: List<Long>,
        connection: Connection,
    ): Map<Long, AnnenReduksjon> {
        if (meldekortIder.isEmpty()) return emptyMap()
        return meldekortIder.chunked(chunkStørrelse).flatMap { chunk ->
            val sql = selectAnmerkningerForMeldekortliste(chunk)
            connection.createStatement().use { statement ->
                statement.executeQuery(sql).map { row ->
                    row.getLong("objekt_id") to AnnenReduksjon(
                        sykedager = row.getFloat("sykedager"),
                        sentMeldekort = row.getFloat("for_sent") > 0,
                        fravær = row.getFloat("fravar"),
                    )
                }
            }
        }.toMap()
    }

    private fun selectBeregningsgrunnlag(vedtakId: Int, connection: Connection): Int {
        log.info("Henter beregningsgrunnlag for vedtak $vedtakId.")
        return connection.prepareStatement(hentBeregningsgrunnlag).use { preparedStatement ->
            preparedStatement.setInt(1, vedtakId)
            val resultSet = preparedStatement.executeQuery()
            var beregningsgrunnlag: Int? = null
            resultSet.map { row ->
                if (row.getString("vedtakfaktakode") == "GRUNN") {
                    beregningsgrunnlag = row.getInt("vedtakverdi")
                }
            }
            beregningsgrunnlag ?: 0
        }
    }

    private fun selectVedtakFakta(vedtakId: Int, connection: Connection): VedtakFakta {
        return connection.prepareStatement(hentVedtakfakta).use { preparedStatement ->
            preparedStatement.setInt(1, vedtakId)
            val resultSet = preparedStatement.executeQuery()
            val vedtakfakta = VedtakFakta(
                dagsmbt = 0,
                barntill = 0,
                dags = 0,
                barnmston = 0,
                dagsfsam = 0,
                justertg = null
            )
            resultSet.map { row ->
                when (row.getString("vedtakfaktakode")) {
                    "DAGSMBT" -> vedtakfakta.dagsmbt = row.getInt("vedtakverdi")
                    "DAGS" -> vedtakfakta.dags = row.getInt("vedtakverdi")
                    "BARNTILL" -> vedtakfakta.barntill = row.getInt("vedtakverdi")
                    "BARNMSTON" -> vedtakfakta.barnmston = row.getInt("vedtakverdi")
                    "DAGSFSAM" -> vedtakfakta.dagsfsam = row.getInt("vedtakverdi")
                    "JUSTERTG" -> vedtakfakta.justertg = row.getString("vedtakverdi")
                }
            }
            vedtakfakta
        }
    }

    private data class MeldekortRad(
        val meldekortId: Long,
        val timerArbeidet: Double,
        val posteringsperiodeFra: LocalDate,
        val posteringsperiodeTil: LocalDate,
        val belop: Int,
    )

    private fun mapTilUtbetaling(
        rad: MeldekortRad,
        anmerkninger: Map<Long, AnnenReduksjon>,
        dagsats: Int,
        barnetillegg: Int,
    ): UtbetalingMedMer = UtbetalingMedMer(
        reduksjon = Reduksjon(
            timerArbeidet = rad.timerArbeidet,
            annenReduksjon = anmerkninger[rad.meldekortId] ?: AnnenReduksjon(0.0f, false, 0.0f),
        ),
        periode = Periode(
            fraOgMedDato = rad.posteringsperiodeFra,
            tilOgMedDato = rad.posteringsperiodeTil,
        ),
        belop = rad.belop,
        dagsats = dagsats,
        barnetillegg = barnetillegg,
    )

    @Language("OracleSql")
    private val selectMaksimumMedTidsbegrensning = """
        SELECT vedtak_id, til_dato, fra_dato, vedtaktypekode, vedtakstatuskode, sak_id, aktfasekode, lopenrvedtak, vedtak_id_relatert, utfallkode
          FROM vedtak 
         WHERE person_id = 
               (SELECT person_id 
                  FROM person 
                 WHERE fodselsnr = ?)
           AND utfallkode = 'JA'
           AND rettighetkode = 'AAP'
           AND vedtaktypekode IN ('O', 'E', 'G', 'S') -- S kan fjernes??
           AND vedtakstatuskode IN ('IVERK', 'AVSLU')
           AND (fra_dato <= til_dato OR til_dato IS NULL)
           AND (til_dato >= ? OR til_dato IS NULL) 
           AND fra_dato <= ?
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
         WHERE vedtak_id = ? AND vedtakfaktakode IN ('DAGSMBT', 'BARNTILL', 'DAGS', 'BARNMSTON', 'DAGSFSAM', 'JUSTERTG', 'SATSBARNTG')
    """.trimIndent()

    // Oracle støtter ikke listeparametere i PreparedStatement, så meldekort-IDer interpoleres direkte.
    private fun selectAnmerkningerForMeldekortliste(meldekortIder: List<Long>): String {
        val idListe = meldekortIder.joinToString(",")
        return """
            SELECT objekt_id,
                   sum(CASE WHEN anmerkningkode = 'FSNN' THEN verdi ELSE 0 END) AS sykedager,
                   sum(CASE WHEN anmerkningkode = 'SENN' THEN verdi ELSE 0 END) AS for_sent,
                   sum(CASE WHEN anmerkningkode = 'FXNN' THEN verdi ELSE 0 END) AS fravar
              FROM anmerkning
             WHERE tabellnavnalias = 'MKORT'
               AND objekt_id IN ($idListe)
               AND anmerkningkode IN ('FSNN', 'SENN', 'FXNN')
             GROUP BY objekt_id
        """.trimIndent()
    }

    @Language("OracleSql")
    private val selectUtbetalingerForPerson = """
        WITH timer AS (
            SELECT mkda.meldekort_id
                 , mkpe.dato_fra - 1 + DECODE( mkda.ukenr, ukenr_uke1 , mkda.dagnr, mkda.dagnr + 7 ) AS dato
                 , mkda.timer_arbeidet
            FROM meldekortdag     mkda
            JOIN meldekort        mkor ON  mkor.meldekort_id = mkda.meldekort_id
            JOIN meldekortperiode mkpe ON  mkpe.aar          = mkor.aar
                                       AND mkpe.periodekode  = mkor.periodekode
            WHERE mkor.person_id = (SELECT person_id FROM person WHERE fodselsnr = ?)
        ),
        utbetalinger AS (
            SELECT p.meldekort_id,
                   p.dato_periode_fra,
                   p.dato_periode_til,
                   SUM(p.belop) AS belop
            FROM postering p
            JOIN meldekort mkor ON mkor.meldekort_id = p.meldekort_id
            JOIN vedtak    v    ON v.vedtak_id       = p.vedtak_id
            WHERE mkor.person_id = (SELECT person_id FROM person WHERE fodselsnr = ?)
              AND v.rettighetkode = 'AAP'
            GROUP BY p.meldekort_id, p.dato_periode_fra, p.dato_periode_til
        )
        SELECT
            SUM( t.timer_arbeidet ) AS timer_arbeidet,
            u.meldekort_id,
            u.belop,
            u.dato_periode_fra,
            u.dato_periode_til
           
        FROM utbetalinger u
        LEFT JOIN timer   t ON  t.meldekort_id = u.meldekort_id
                            AND t.dato   BETWEEN u.dato_periode_fra AND u.dato_periode_til
        GROUP BY u.meldekort_id,
                 u.belop,
                 u.dato_periode_fra,
                 u.dato_periode_til
        ORDER BY dato_periode_fra, meldekort_id
    """.trimIndent()
}
