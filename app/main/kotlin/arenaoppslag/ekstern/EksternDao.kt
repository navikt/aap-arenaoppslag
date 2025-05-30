package arenaoppslag.ekstern

import arenaoppslag.datasource.map
import arenaoppslag.modeller.*
import java.sql.Connection
import java.sql.Date
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

//https://confluence.adeo.no/display/ARENA/Arena+-+Datamodell+-+Vedtak

object EksternDao {
    private const val selectMaksimumMedTidsbegrensning = """
        SELECT vedtak_id, til_dato, fra_dato, vedtaktypekode, vedtakstatuskode, sak_id, aktfasekode 
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
    """

    private const val hentBeregningsgrunnlag = """
        SELECT vedtakfaktakode, vedtakverdi
            FROM vedtakfakta 
             WHERE vedtak_id = ? AND vedtakfaktakode IN ('GRUNN')
    """

    private const val hentVedtakfakta = """
        SELECT vedtakfaktakode, vedtakverdi
            FROM vedtakfakta 
             WHERE vedtak_id = ? AND vedtakfaktakode IN ('DAGSMBT', 'BARNTILL', 'DAGS', 'BARNMSTON', 'DAGSFSAM')
    """

    private const val selectVedtakMedTidsbegrensningSql = """
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
    """

    private const val selectSykedagerMeldekort = """
        SELECT sum(verdi)
          FROM anmerkning
        WHERE tabellnavnalias = 'MKORT'
          AND objekt_id       = ?
          AND anmerkningkode  = 'FSNN'
    """

    private const val selectSentMeldekort = """
        SELECT sum(verdi)
          FROM anmerkning
        WHERE tabellnavnalias = 'MKORT'
          AND objekt_id       = ?
          AND anmerkningkode  = 'SENN'
    """

    private const val selectFraværMeldekort = """
        SELECT sum(verdi)
          FROM anmerkning
        WHERE tabellnavnalias = 'MKORT'
          AND objekt_id       = ?
          AND anmerkningkode  = 'FXNN'
    """
    //Syk=FSNN', fravære = 'FXNN' og for sent = 'SENN'

    //henter timer arbeidet for bruker x mellom y og z dato gruppert på meldekortperiode
    private const val selectTimerArbeidetIMeldekortPeriode = """
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
    """

    private fun selectSykedagerMeldekort(meldekortId: String, connection: Connection): Int {
        return connection.prepareStatement(selectSykedagerMeldekort).use { preparedStatement ->
            preparedStatement.setString(1, meldekortId)

            val resultSet = preparedStatement.executeQuery()

            resultSet.map { row ->
                row.getInt(1)
            }.firstOrNull() ?: 0
        }
    }

    fun selectFraværMeldekort(meldekortId: String, connection: Connection): Int {
        return connection.prepareStatement(selectFraværMeldekort).use { preparedStatement ->
            preparedStatement.setString(1, meldekortId)

            val resultSet = preparedStatement.executeQuery()

            resultSet.map { row ->
                row.getInt(1)
            }.firstOrNull() ?: 0
        }
    }

    fun selectSentMeldekort(meldekortId: String, connection: Connection): Boolean {
        return connection.prepareStatement(selectSentMeldekort).use { preparedStatement ->
            preparedStatement.setString(1, meldekortId)

            val resultSet = preparedStatement.executeQuery()

            val forSent = resultSet.map { row ->
                row.getInt(1)
            }.firstOrNull() ?: 0
            forSent > 0
        }
    }


    fun selectVedtakMinimum(
        personId: String,
        fraOgMedDato: LocalDate,
        tilOgMedDato: LocalDate,
        connection: Connection
    ): List<Periode> {
        return connection.prepareStatement(selectVedtakMedTidsbegrensningSql)
            .use { preparedStatement ->
                preparedStatement.setString(1, personId)
                preparedStatement.setDate(2, Date.valueOf(fraOgMedDato))
                preparedStatement.setDate(3, Date.valueOf(tilOgMedDato))

                val resultSet = preparedStatement.executeQuery()

                val perioder = resultSet.map { row ->

                    Periode(
                        fraOgMedDato = row.getDate("fra_dato").toLocalDate(),
                        tilOgMedDato = getNullableDate(row.getDate("til_dato")),
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
        fra_Dato: LocalDate,
        til_dato: LocalDate
    ): List<UtbetalingMedMer> {
        return connection.prepareStatement(selectTimerArbeidetIMeldekortPeriode)
            .use { preparedStatement ->
                preparedStatement.setInt(1, vedtakId)
                preparedStatement.setString(2, personId)
                preparedStatement.setDate(3, Date.valueOf(fra_Dato))
                preparedStatement.setDate(4, Date.valueOf(til_dato))

                val resultSet = preparedStatement.executeQuery()

                resultSet.map { row ->
                    //hent andmerking for sent meldekort
                    val meldekortId = row.getString("meldekort_id")
                    UtbetalingMedMer(
                        reduksjon = Reduksjon(
                            timerArbeidet = row.getFloat("timer_arbeidet").toDouble(),
                            annenReduksjon = AnnenReduksjon(
                                selectSykedagerMeldekort(meldekortId, connection).toFloat(),
                                selectSentMeldekort(meldekortId, connection),
                                selectFraværMeldekort(meldekortId, connection).toFloat()
                            )
                        ),
                        periode = Periode(
                            fraOgMedDato = row.getDate("dato_periode_fra").toLocalDate(),
                            tilOgMedDato = row.getDate("dato_periode_til").toLocalDate(),
                        ),
                        belop = row.getInt("belop"),
                        dagsats = dagsats,
                        barnetilegg = barnetiTillegg
                    )
                }.toList()
            }
    }

    fun selectBeregningsgrunnlag(vedtakId: Int, connection: Connection): Int {
        return connection.prepareStatement(hentBeregningsgrunnlag).use { preparedStatement ->
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
        return connection.prepareStatement(hentVedtakfakta).use { preparedStatement ->
            preparedStatement.setInt(1, vedtakId)
            val resultSet = preparedStatement.executeQuery()
            val vedtakfakta=VedtakFakta(0, 0, 0, 0, 0)
            resultSet.map { row ->
                when (row.getString("vedtakfaktakode")) {
                    "DAGSMBT" -> vedtakfakta.dagsmbt = row.getInt("vedtakverdi")
                    "BARNTILL" -> vedtakfakta.barntill = row.getInt("vedtakverdi")
                    "DAGS" -> vedtakfakta.dags = row.getInt("vedtakverdi")
                    "DAGSFSAM" -> vedtakfakta.dagsfs = row.getInt("vedtakverdi")
                    "BARNMSTON" -> vedtakfakta.barnmston = row.getInt("vedtakverdi")
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
        val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val maksimum =
            connection.prepareStatement(selectMaksimumMedTidsbegrensning).use { preparedStatement ->
                preparedStatement.setString(1, personId)
                preparedStatement.setDate(2, Date.valueOf(fraOgMedDato))
                preparedStatement.setDate(3, Date.valueOf(tilOgMedDato))

                val resultSet = preparedStatement.executeQuery()
                val utbetalinger = mutableListOf<UtbetalingMedMer>()
                val vedtak = resultSet.map { row ->
                    val vedtakId = row.getInt("vedtak_id")
                    val vedtakFakta = selectVedtakFakta(vedtakId, connection)
                    utbetalinger.addAll(
                        selectUtbetalingVedVedtakId(
                            connection = connection,
                            barnetiTillegg = vedtakFakta.barntill,
                            dagsats = vedtakFakta.dags,
                            personId = personId,
                            vedtakId = row.getInt("vedtak_id"),
                            fra_Dato = fraOgMedDato,
                            til_dato = tilOgMedDato
                        )
                    )
                    Vedtak(
                        vedtaksId = vedtakId.toString(),
                        utbetaling = utbetalinger,
                        dagsats = vedtakFakta.dagsfs,
                        status = row.getString("vedtakstatuskode"),
                        saksnummer = row.getString("sak_id"),
                        vedtaksdato = LocalDateTime.parse(row.getString("fra_dato"), inputFormatter).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        rettighetsType = row.getString("aktfasekode"),
                        periode = Periode(
                            fraOgMedDato = row.getDate("fra_dato").toLocalDate(),
                            tilOgMedDato = getNullableDate(row.getDate("til_dato"))
                        ),
                        beregningsgrunnlag = selectBeregningsgrunnlag(vedtakId, connection),
                        barnMedStonad = vedtakFakta.barnmston,
                        vedtaksTypeKode = row.getString("vedtaktypekode"),
                        vedtaksTypeNavn = VedtaksType.entries.find { it.kode == row.getString("vedtaktypekode") }?.navn
                            ?: ""
                    )
                }.toList()
                Maksimum(vedtak)
            }
        return maksimum
    }

    private fun getNullableDate(date: Date?): LocalDate? {
        if (date == null) return null
        return date.toLocalDate()
    }
}
