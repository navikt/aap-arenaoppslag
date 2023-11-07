package arenaoppslag.dsop

import arenaoppslag.dao.forEach
import arenaoppslag.dao.map
import arenaoppslag.fellesordningen.VedtakPeriode
import java.sql.Date
import javax.sql.DataSource

class DsopDao(private val dataSource: DataSource) {
    private val selectMeldekortDagSql = """
        SELECT * from v_dsop_meldekortdag_aap
        WHERE meldekortid =?
         
    """
    private val selectMeldekortSql = """
        SELECT * from v_dsop_meldekort_aap
        WHERE foedselsnr =? 
        AND datofra <= ? 
        AND datotil >= ?   
    """

    private val selectVedtakSql = """
        SELECT * from v_dsop_vedtak_aap
        WHERE foedselsnr =?
        AND datofra <= ?
        AND datotil >= ?
    """

    fun selectVedtak(personId: String, periode: Periode, samtykkePeriode: Periode): VedtakResponse {
        return dataSource.connection.use { connection ->
            connection.prepareStatement(selectVedtakSql).use { preparedStatement ->
                preparedStatement.setString(1, personId)
                preparedStatement.setDate(2, Date.valueOf(periode.fraDato))
                preparedStatement.setDate(3, Date.valueOf(periode.tilDato))

                val resultSet = preparedStatement.executeQuery()

                val vedtaksListe = resultSet.map { row ->
                    val fraDato = row.getDate("datofra").toLocalDate()
                    val tilDato = row.getDate("datotil").toLocalDate()

                    val justertFraDato =
                        if (fraDato.isBefore(samtykkePeriode.fraDato)) samtykkePeriode.fraDato else fraDato
                    val justertTilDato =
                        if (tilDato.isAfter(samtykkePeriode.tilDato)) samtykkePeriode.tilDato else tilDato
                    DsopVedtak(
                        vedtakId = row.getInt("vedtakid"),
                        virkningsperiode = Periode(
                            fraDato = justertFraDato,
                            tilDato = justertTilDato
                        ),
                        vedtakstype = Kodeverdi(
                            kode = row.getString("vedtakstypekode"),
                            termnavn = row.getString("vedtakstypenavn")
                        ),
                        vedtaksvariant = Kodeverdi(
                            kode = row.getString("vedtaksvariantkode"),
                            termnavn = row.getString("vedtaksvariantnavn")
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
                            kode = row.getString("aktivitetsfasekode"),
                            termnavn = row.getString("aktivitetsfasenavn")
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
                preparedStatement.setDate(2, Date.valueOf(periode.fraDato))
                preparedStatement.setDate(3, Date.valueOf(periode.tilDato))

                val resultSet = preparedStatement.executeQuery()

                val meldekortListe = resultSet.map { row ->
                    val fraDato = row.getDate("datofra").toLocalDate()
                    val tilDato = row.getDate("datotil").toLocalDate()

                    val justertFraDato =
                        if (fraDato.isBefore(samtykkePeriode.fraDato)) samtykkePeriode.fraDato else fraDato
                    val justertTilDato =
                        if (tilDato.isAfter(samtykkePeriode.tilDato)) samtykkePeriode.tilDato else tilDato
                    DsopMeldekort(
                        meldekortId = row.getInt("meldekortid"),
                        periode = Periode(
                            fraDato = justertFraDato,
                            tilDato = justertTilDato
                        ),
                        antallTimerArbeidet = kalkulerAntallTimerArbeidet(row.getInt("meldekortid"), samtykkePeriode),
                    )
                }.toList()
                MeldekortResponse(periode, meldekortListe)
            }
        }
    }

    fun kalkulerAntallTimerArbeidet(meldekortId: Int, samtykkePeriode: Periode ): Double {
        return dataSource.connection.use { connection ->
            connection.prepareStatement(selectMeldekortDagSql).use { preparedStatement ->
                preparedStatement.setInt(1, meldekortId)

                val resultSet = preparedStatement.executeQuery()

                var antallTimerArbeidet = 0.0
                resultSet.forEach {
                    val dato= it.getDate("dato").toLocalDate()

                    if (dato.isAfter(samtykkePeriode.fraDato) && dato.isBefore(samtykkePeriode.tilDato)) {
                        antallTimerArbeidet += it.getDouble("timer_arbeidet")
                    }
                }

                antallTimerArbeidet
            }
        }
    }
}

