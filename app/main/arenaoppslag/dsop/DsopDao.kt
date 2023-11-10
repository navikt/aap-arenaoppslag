package arenaoppslag.dsop

import arenaoppslag.dao.forEach
import arenaoppslag.dao.map
import java.sql.Date
import java.time.LocalDate
import javax.sql.DataSource

class DsopDao(private val dataSource: DataSource) {
    private val selectMeldekortDagSql = """
        SELECT * from v_dsop_meldekortdag_aap
        WHERE meldekort_id =?
         
    """
    private val selectMeldekortSql = """
        SELECT * from v_dsop_meldekort_aap
        WHERE fodselsnr =? 
        AND dato_fra <= ? 
        AND dato_til >= ?   
    """

    private val selectVedtakSql = """
        SELECT * from v_dsop_vedtak_aap
        WHERE fodselsnr =?
        AND fra_dato <= ?
        AND til_dato >= ?
    """

    fun selectVedtak(personId: String, periode: Periode, samtykkePeriode: Periode): VedtakResponse {
        return dataSource.connection.use { connection ->
            connection.prepareStatement(selectVedtakSql).use { preparedStatement ->
                preparedStatement.setString(1, personId)
                preparedStatement.setDate(2, Date.valueOf(periode.tilDato))
                preparedStatement.setDate(3, Date.valueOf(periode.fraDato))

                val resultSet = preparedStatement.executeQuery()

                val vedtaksListe = resultSet.map { row ->
                    val justertFraDato = justerFradato(row.getDate("fra_dato").toLocalDate(), samtykkePeriode)
                    val justertTilDato = justerTilDato(row.getDate("til_dato").toLocalDate(), samtykkePeriode)

                    DsopVedtak(
                        vedtakId = row.getInt("vedtak_id"),
                        virkningsperiode = Periode(
                            fraDato = justertFraDato,
                            tilDato = justertTilDato
                        ),
                        vedtakstype = Kodeverdi(
                            kode = row.getString("vedtaktypekode"),
                            termnavn = row.getString("vedtaktypenavn")
                        ),
                        vedtaksvariant = Kodeverdi(
                            kode = row.getString("tekstvariantkode"),
                            termnavn = row.getString("tekstvariantnavn")
                        ),
                        vedtakstatus = Kodeverdi(
                            kode = row.getString("vedtakstatuskode"),
                            termnavn = row.getString("vedtakstatusnavn")
                        ),
                        rettighetstype = Kodeverdi(
                            kode = row.getString("rettighetkode"),
                            termnavn = row.getString("rettighetnavn")
                        ),
                        utfall = Kodeverdi(
                            kode = row.getString("utfallkode"),
                            termnavn = row.getString("utfallnavn")
                        ),
                        aktivitetsfase = Kodeverdi(
                            kode = row.getString("aktfasekode"),
                            termnavn = row.getString("aktfasenavn")
                        )
                    )
                }.toList()
                VedtakResponse(periode, vedtaksListe)
            }
        }
    }

    fun selectMeldekort(personId: String, periode: Periode, samtykkePeriode: Periode): MeldekortResponse {
        return dataSource.connection.use { connection ->
            connection.prepareStatement(selectMeldekortSql).use { preparedStatement ->
                preparedStatement.setString(1, personId)
                preparedStatement.setDate(2, Date.valueOf(periode.tilDato))
                preparedStatement.setDate(3, Date.valueOf(periode.fraDato))

                val resultSet = preparedStatement.executeQuery()

                val meldekortListe = resultSet.map { row ->
                    val justertFraDato = justerFradato(row.getDate("dato_fra").toLocalDate(), samtykkePeriode)
                    val justertTilDato = justerTilDato(row.getDate("dato_til").toLocalDate(), samtykkePeriode)

                    val meldekortId = row.getInt("meldekort_id")
                    DsopMeldekort(
                        meldekortId = meldekortId,
                        periode = Periode(
                            fraDato = justertFraDato,
                            tilDato = justertTilDato
                        ),
                        antallTimerArbeidet = kalkulerAntallTimerArbeidet(meldekortId, samtykkePeriode),
                    )
                }.toList()
                MeldekortResponse(periode, meldekortListe)
            }
        }
    }

    private fun kalkulerAntallTimerArbeidet(meldekortId: Int, samtykkePeriode: Periode): Double {
        return dataSource.connection.use { connection ->
            connection.prepareStatement(selectMeldekortDagSql).use { preparedStatement ->
                preparedStatement.setInt(1, meldekortId)

                val resultSet = preparedStatement.executeQuery()

                var antallTimerArbeidet = 0.0
                resultSet.forEach {
                    val dato = it.getDate("dato").toLocalDate()

                    if (dato in samtykkePeriode.fraDato .. samtykkePeriode.tilDato) {
                        antallTimerArbeidet += it.getDouble("timer_arbeidet")
                    }
                }

                antallTimerArbeidet
            }
        }
    }

    private fun justerFradato(fraDato: LocalDate, samtykkePeriode: Periode) =
        if (fraDato.isBefore(samtykkePeriode.fraDato)) samtykkePeriode.fraDato else fraDato

    private fun justerTilDato(tilDato: LocalDate, samtykkePeriode: Periode) =
        if (tilDato.isAfter(samtykkePeriode.tilDato)) samtykkePeriode.tilDato else tilDato
}

