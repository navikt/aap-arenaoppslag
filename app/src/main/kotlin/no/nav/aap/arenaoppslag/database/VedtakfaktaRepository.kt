package no.nav.aap.arenaoppslag.database

import org.intellij.lang.annotations.Language
import java.time.LocalDate
import javax.sql.DataSource

class VedtakfaktaRepository(private val dataSource: DataSource) {

    fun `hentMaksdatoEtterUtløpAvKvoteForSak`(sakId: Int): LocalDate? {
        return dataSource.connection.use { connection ->
            connection.createParameterizedQuery(selectMaksDatoForSak).use { preparedStatement ->
                preparedStatement.setInt(1, sakId)
                val result = preparedStatement.executeQuery()

                if (!result.next()) {
                    // saken ble ikke funnet, eller saken har ikke maksdatoer for AAP-vedtaket
                    return null
                } else {
                    val ordinærMaksdato = result.getDate("maksdato")?.toLocalDate()
                    val utvidetMaksDato = result.getDate("utvidet_maksdato")?.toLocalDate()

                    utvidetMaksDato ?: ordinærMaksdato
                }
            }
        }
    }

    companion object {

        /*
        Verdiene finnes bare i Arena for vedtak med rettighetkode='AAP' og vedtaktypekode IN ('O', 'E', 'S', 'G').
        Verdiene er like i alle vedtakene som kommer etter vedtaket det først ble registrert på.
        Bare når maksdato er passert, altså at aap-kvoten er nede i null, skrives den til vedtaksfakta.
        I tilfellet hvor utvidet kvote ikke er i bruk (enda) er utvidet_maksdato null.
        CASE brukes til å hente ut begge de to verdiene i samme result row. Det er bare to verdier for samme vedtak_id.
         */
        @Language("OracleSql")
        val selectMaksDatoForSak = """
        SELECT
            v.sak_id,
            MAX(CASE WHEN vf.vedtakfaktakode = 'MAXDATOAAP'  THEN vf.vedtakverdi END) AS maksdato,
            MAX(CASE WHEN vf.vedtakfaktakode = 'MAXDATOUNT' THEN vf.vedtakverdi END) AS utvidet_maksdato
        FROM vedtak v
        JOIN vedtakfakta vf 
            ON vf.vedtak_id = v.vedtak_id
        WHERE v.vedtak_id = (
                SELECT v2.vedtak_id
                FROM vedtak v2
                WHERE v2.sak_id = ?
                  AND v2.rettighetkode = 'AAP'
                  AND v2.vedtaktypekode IN ('O', 'E', 'S', 'G')
                  -- filtrer ut ugyldiggjorte vedtak
                  AND NOT (fra_dato > til_dato AND (til_dato IS NOT NULL AND fra_dato IS NOT NULL))
                  -- filtrer ut avbrutte vedtak
                  AND (v.utfallkode IS NULL OR v.utfallkode != 'AVBRUTT')
                -- ta nyeste gyldige AAP vedtak i saken
                ORDER BY v2.reg_dato DESC
                FETCH FIRST 1 ROW ONLY
            )
          AND vf.vedtakfaktakode IN ('MAXDATOAAP', 'MAXDATOUNT')
        GROUP BY v.sak_id;
    """.trimIndent()
    }

}
