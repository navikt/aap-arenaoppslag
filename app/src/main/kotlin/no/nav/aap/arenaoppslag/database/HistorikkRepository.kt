package no.nav.aap.arenaoppslag.database

import no.nav.aap.arenaoppslag.database.DbDato.fraDato
import no.nav.aap.arenaoppslag.modeller.ArenaVedtak
import no.nav.aap.arenaoppslag.modeller.PersonId
import org.intellij.lang.annotations.Language
import java.sql.Connection
import java.sql.Date
import java.sql.ResultSet
import java.time.LocalDate
import javax.sql.DataSource

class HistorikkRepository(private val dataSource: DataSource) {

    fun hentAlleSignifikanteVedtakForPerson(
        arenaPersonId: PersonId, søknadMottattPå: LocalDate
    ): List<ArenaVedtak> {
        return dataSource.connection.use { con ->
            hentAlleSignifikanteVedtakForPerson(arenaPersonId.id, søknadMottattPå, con)
        }
    }

    companion object {

        // Henter alle vedtak med relevant AAP-historikk for personen
        @Language("OracleSql")
        val selectSignifikanteHistoriskeVedtak = """
            WITH ubehandlede_aa115_vedtak AS (SELECT sak_id,
                                             vedtak_id,
                                             aar,
                                             lopenrvedtak,
                                             lopenrsak,
                                             vedtakstatuskode,
                                             vedtaktypekode,
                                             fra_dato,
                                             til_dato,
                                             rettighetkode,
                                             aktfasekode,
                                             utfallkode
                                      FROM vedtak v
                                      WHERE v.person_id = ?
                                        AND v.rettighetkode = 'AA115'
                                        AND v.utfallkode IS NULL -- ikke behandlet enda
                                        AND v.MOD_DATO >= ? -- ikke utdatert
        ),
        ikke_stansede_aap_vedtak AS (SELECT sak_id,
                                       vedtak_id,
                                       aar,
                                       lopenrvedtak,
                                       lopenrsak,
                                       vedtakstatuskode,
                                       vedtaktypekode,
                                       fra_dato,
                                       til_dato,
                                       rettighetkode,
                                       aktfasekode,
                                       utfallkode
                                FROM vedtak v
                                WHERE v.person_id = ?
                                  AND (v.utfallkode IS NULL OR v.utfallkode = 'JA')
                                  AND v.rettighetkode = 'AAP'
                                  AND v.MOD_DATO >= ? -- ytelse: unngå å løpe gjennom veldig gamle vedtak
                                  AND NOT (fra_dato > til_dato AND (til_dato IS NOT NULL AND fra_dato IS NOT NULL)) -- filtrer ut ugyldiggjorte vedtak
                                  AND ((fra_dato IS NOT NULL OR til_dato IS NOT NULL) OR
                                       vedtakstatuskode IN ('OPPRE', 'MOTAT', 'REGIS', 'INNST')) -- filtrer ut etterregistrerte vedtak, men behold vedtak som er under behandling
                                  AND (
                                    ((vedtaktypekode IN ('O', 'E', 'G') OR (vedtaktypekode = 'S' and v.til_dato IS NOT NULL))
                                        AND
                                     (til_dato IS NULL OR til_dato >= ?)) -- vanlig tidsbuffer
                                    )
         ),
         stansede_aap_vedtak AS (SELECT sak_id,
                                        vedtak_id,
                                        aar,
                                        lopenrvedtak,
                                        lopenrsak,
                                        vedtakstatuskode,
                                        vedtaktypekode,
                                        fra_dato,
                                        til_dato,
                                        rettighetkode,
                                        aktfasekode,
                                        utfallkode
                                 FROM vedtak v
                                 WHERE v.person_id = ?
                                   AND (v.utfallkode IS NULL OR v.utfallkode = 'JA')
                                   AND v.rettighetkode = 'AAP'
                                   AND v.MOD_DATO >= ? -- ytelse: unngå å løpe gjennom veldig gamle vedtak
                                   AND NOT (fra_dato > til_dato AND (til_dato IS NOT NULL AND fra_dato IS NOT NULL)) -- filtrer ut ugyldiggjorte vedtak
                                   AND ((fra_dato IS NOT NULL OR til_dato IS NOT NULL) OR
                                        vedtakstatuskode IN ('OPPRE', 'MOTAT', 'REGIS', 'INNST'))  -- filtrer ut etterregistrerte vedtak, men behold vedtak som er under behandling
                                   AND (
                                     (vedtaktypekode = 'S' AND til_dato IS NULL AND
                                      (fra_dato IS NULL OR fra_dato >= ?)) -- ekstra tidsbuffer for Stans, som bare har fra_dato
                                     )
         ),
         siste_lopende_vedtak as (SELECT sak_id,
                                      vedtak_id,
                                      aar,
                                      lopenrvedtak,
                                      lopenrsak,
                                      vedtakstatuskode,
                                      vedtaktypekode,
                                      fra_dato,
                                      til_dato,
                                      rettighetkode,
                                      aktfasekode,
                                      utfallkode
                               FROM ikke_stansede_aap_vedtak 
                               WHERE vedtakstatuskode IN ('IVERK', 'AVSLU')
                               ORDER by til_dato DESC NULLS LAST
                                   FETCH FIRST 1 ROW ONLY
         ),
         siste_stansede_aap_vedtak as (SELECT sak_id,
                                          vedtak_id,
                                          aar,
                                          lopenrvedtak,
                                          lopenrsak,
                                          vedtakstatuskode,
                                          vedtaktypekode,
                                          fra_dato,
                                          til_dato,
                                          rettighetkode,
                                          aktfasekode,
                                          utfallkode
                                   FROM stansede_aap_vedtak as stansede
                                   WHERE stansede.sak_id in (SELECT sak_id from siste_lopende_vedtak as siste
                                      WHERE
                                         -- Stansen må komme etter det siste løpende vedtaket:
                                         stansede.vedtak_id > siste.vedtak_id -- et nyere vedtak
                                         AND (siste.fra_dato IS NOT NULL AND stansede.fra_dato IS NOT NULL AND stansede.fra_dato > siste.fra_dato) -- med nyere fra_dato
                                   )
                                   ORDER BY fra_dato DESC NULLS LAST
                                       FETCH FIRST 1 ROW ONLY
         )
            -- Kombiner dem:
            SELECT *
            FROM ubehandlede_aa115_vedtak
               UNION ALL
            SELECT *
            FROM ikke_stansede_aap_vedtak
               UNION ALL
            SELECT *
            FROM siste_stansede_aap_vedtak     
        """.trimIndent()

        const val vanligTidsbufferUker = 78L // 52 uker + 6 måneder tilbakejustering
        const val stansTidsbufferUker = 119L // foreldrepenger med 80% utbetalt, trillinger, alenemor
        const val aa115BehandlingUker = 26L // maksimal behandlingstid vi regner for AA115-vedtak
        const val modnedGrenseVedtak = 72L

        fun hentAlleSignifikanteVedtakForPerson(
            arenaPersonId: Int, søknadMottattPå: LocalDate, connection: Connection
        ): List<ArenaVedtak> {
            val vanligTidsbuffer = Date.valueOf(søknadMottattPå.minusWeeks(vanligTidsbufferUker))
            val stansTidsbuffer = Date.valueOf(søknadMottattPå.minusWeeks(stansTidsbufferUker))
            val vedtakModnedGrense = Date.valueOf(søknadMottattPå.minusMonths(modnedGrenseVedtak))
            val aa115BehandlingUkerGrense = Date.valueOf(søknadMottattPå.minusWeeks(aa115BehandlingUker))

            connection.createParameterizedQuery(selectSignifikanteHistoriskeVedtak).use { preparedStatement ->
                var p = 1 // parameter-indeks
                // ubehandlede_aa115_vedtak
                preparedStatement.setInt(p++, arenaPersonId)
                preparedStatement.setDate(p++, aa115BehandlingUkerGrense)
                // ikke_stansede_aap_vedtak
                preparedStatement.setInt(p++, arenaPersonId)
                preparedStatement.setDate(p++, vedtakModnedGrense)
                preparedStatement.setDate(p++, vanligTidsbuffer)
                // stansede_aap_vedtak
                preparedStatement.setInt(p++, arenaPersonId)
                preparedStatement.setDate(p++, vedtakModnedGrense)
                preparedStatement.setDate(p++, stansTidsbuffer)

                val resultSet = preparedStatement.executeQuery()
                return resultSet.map { row -> mapperForArenaVedtak(row) }
            }
        }

        fun mapperForArenaVedtak(row: ResultSet) = ArenaVedtak(
            sakId = row.getString("sak_id"),
            aar = row.getInt("aar"),
            lopenrvedtak = row.getInt("lopenrvedtak"),
            statusKode = row.getString("vedtakstatuskode"),
            vedtaktypeKode = row.getString("vedtaktypekode"),
            fraOgMed = fraDato(row.getDate("fra_dato")),
            tilDato = fraDato(row.getDate("til_dato")),
            rettighetkode = row.getString("rettighetkode"),
            utfallkode = row.getString("utfallkode"),
            aktivitetsfaseKode = row.getString("aktfasekode"),
        )

    }

}
