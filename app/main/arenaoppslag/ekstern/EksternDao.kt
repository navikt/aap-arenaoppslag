package arenaoppslag.ekstern

import arenaoppslag.datasource.map
import arenaoppslag.modeller.*
import arenaoppslag.perioder.Periode
import java.sql.Connection
import java.sql.Date
import java.time.LocalDate

//https://confluence.adeo.no/display/ARENA/Arena+-+Datamodell+-+Vedtak

object EksternDao {
    private const val selectMaksimumMedTidsbegrensning = """
        SELECT vedtak_id, til_dato, fra_dato, vedtakstatuskode, sak_id, aktfasekode 
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

    private const val hentVedtakfakta = """
        SELECT vedtakfaktakode, vedtakverdi
            FROM vedtakfakta 
             WHERE vedtak_id = ? AND vedtakfaktakode IN ('DAGSMBT', 'BARNTILL', 'DAGS')
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

    private const val selectAnmerkningsTyper = """
        SELECT DISTINCT anmerkningkode, anmerkningnavn
          FROM anmerkningtype
          """

    //henter timer arbeidet for bruker x mellom y og z dato gruppert på meldekortperiode
    private const val selectTimerArbeidetIMeldekortPeriode = """
        SELECT 
            SUM(mkd.timer_arbeidet) AS timer_arbeidet,
            m.meldekort_id,
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
            p.dato_periode_fra, p.dato_periode_til, p.belop
    """


    fun selectAnmerkningTyper(connection: Connection): List<AnmerkningType> {
        return connection.prepareStatement(selectAnmerkningsTyper).use { preparedStatement ->
            val resultSet = preparedStatement.executeQuery()

            resultSet.map { row ->
                AnmerkningType(
                    kode = row.getString("anmerkningkode"),
                    navn = row.getString("anmerkningnavn")
                )
            }.toList()
        }
    }


    fun selectVedtakMinimum(
        personId: String,
        fraOgMedDato: LocalDate,
        tilOgMedDato: LocalDate,
        connection: Connection
    ): List<Minimum> {
        return connection.prepareStatement(selectVedtakMedTidsbegrensningSql).use { preparedStatement ->
            preparedStatement.setString(1, personId)
            preparedStatement.setDate(2, Date.valueOf(fraOgMedDato))
            preparedStatement.setDate(3, Date.valueOf(tilOgMedDato))

            val resultSet = preparedStatement.executeQuery()

            resultSet.map { row ->
                Minimum(
                    Periode(
                        fraOgMedDato = row.getDate("fra_dato").toLocalDate(),
                        tilOgMedDato = getNullableDate(row.getDate("til_dato")),
                    )
                )
            }.toList()
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
        return connection.prepareStatement(selectTimerArbeidetIMeldekortPeriode).use { preparedStatement ->
            preparedStatement.setInt(1, vedtakId)
            preparedStatement.setString(2, personId)
            preparedStatement.setDate(3,Date.valueOf(fra_Dato))
            preparedStatement.setDate(4,Date.valueOf(til_dato))

            val resultSet = preparedStatement.executeQuery()

            return resultSet.map { row ->
                //hent andmerking for sent meldekort
                UtbetalingMedMer(
                    reduksjon = Reduksjon(
                        timerArbeidet = row.getFloat("timer_arbeidet").toDouble(),
                        annenReduksjon = AnnenReduksjon(null,null,null)
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

    fun selectVedtakFakta(vedtakId: Int, connection: Connection): VedtakFakta{
        return connection.prepareStatement(hentVedtakfakta).use { preparedStatement ->
            preparedStatement.setInt(1, vedtakId)
            val resultSet = preparedStatement.executeQuery()
            val vedtakfakta=VedtakFakta(0, 0, 0)
            resultSet.map { row ->
                when(row.getString("vedtakfaktakode")){
                    "DAGSMBT" -> vedtakfakta.dagsmbt = row.getInt("vedtakverdi")
                    "BARNTILL" -> vedtakfakta.barntill = row.getInt("vedtakverdi")
                    "DAGS" -> vedtakfakta.dags = row.getInt("vedtakverdi")
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
    ):Maksimum2{
        val maksimum = connection.prepareStatement(selectMaksimumMedTidsbegrensning).use { preparedStatement ->
            preparedStatement.setString(1, personId)
            preparedStatement.setDate(2, Date.valueOf(fraOgMedDato))
            preparedStatement.setDate(3, Date.valueOf(tilOgMedDato))

            val resultSet = preparedStatement.executeQuery()
            val utbetalinger = mutableListOf<UtbetalingMedMer>()
            val vedtak = resultSet.map { row ->
                val vedtakFakta = selectVedtakFakta(row.getInt("vedtak_id"),connection)
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
                    utbetaling = utbetalinger,
                    dagsats = vedtakFakta.dags,
                    status = row.getString("vedtakstatuskode"),
                    saksnummer = row.getString("sak_id"),
                    vedtaksdato = row.getString("fra_dato"),
                    rettighetType = row.getString("aktfasekode"),
                    periode = Periode(
                        fraOgMedDato = row.getDate("fra_dato").toLocalDate(),
                        tilOgMedDato = getNullableDate(row.getDate("til_dato"))
                    ),
                )
            }.toList()
            Maksimum2(vedtak, utbetalinger)
        }
        return maksimum
    }

    private fun getNullableDate(date: Date?): LocalDate? {
        if (date == null) return null
        return date.toLocalDate()
    }
}

data class AnmerkningType(val kode: String?, val navn: String?)
