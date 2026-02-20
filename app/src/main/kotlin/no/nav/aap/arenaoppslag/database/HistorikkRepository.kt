package no.nav.aap.arenaoppslag.database

import no.nav.aap.arenaoppslag.database.DbDato.fraDato
import no.nav.aap.arenaoppslag.modeller.ArenaVedtak
import org.intellij.lang.annotations.Language
import java.sql.Connection
import java.sql.Date
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.time.LocalDate
import javax.sql.DataSource

class HistorikkRepository(private val dataSource: DataSource) {

    fun `hentIkkeAvbrutteVedtakSisteFemÅrForPerson`(personId: Int): List<ArenaVedtak> {
        dataSource.connection.use { connection ->
            connection.createParameterizedQuery(selectIkkeAvbrutteSisteFemÅr)
                .use { preparedStatement ->
                    var p = 1 // parameter-indeks
                    // vedtak
                    preparedStatement.setInt(p++, personId)
                    // spesialutbetalinger
                    preparedStatement.setInt(p++, personId)
                    // sim_utbetalingsgrunnlag
                    preparedStatement.setInt(p++, personId)

                    val resultSet = preparedStatement.executeQuery()
                    return resultSet.map { row -> mapperForArenaVedtak(row) }
                }

        }
    }

    fun hentAlleSignifikanteVedtakForPerson(
        personId: Int, `søknadMottattPå`: LocalDate
    ): List<ArenaVedtak> {
        return dataSource.connection.use { con ->
            hentAlleSignifikanteVedtakForPerson(personId, søknadMottattPå, con)
        }
    }

    companion object {

        private fun Connection.createParameterizedQuery(queryString: String): PreparedStatement {
            val query = prepareStatement(queryString)
            query.queryTimeout = 300 // set a timeout in seconds, to avoid long running queries
            return query
        }

        @Language("OracleSql")
        val selectIkkeAvbrutteSisteFemÅr = """            
        SELECT sak_id, vedtakstatuskode, vedtaktypekode, fra_dato, til_dato, rettighetkode
        FROM
            vedtak v
        
        WHERE v.person_id = ?
          AND (v.utfallkode IS NULL OR v.utfallkode != 'AVBRUTT')
          AND v.rettighetkode IN ('AA115', 'AAP', 'KLAG1', 'KLAG2', 'ANKE', 'TILBBET')
          AND v.MOD_DATO >= ADD_MONTHS(TRUNC(SYSDATE), -60)
        UNION ALL
        
        SELECT
            v.sak_id,
            su.vedtakstatuskode,
            'O'  AS vedtaktypekode,
            su.dato_fra AS fra_dato,
            su.dato_til AS til_dato,
            'SPESIAL' AS rettighetkode
        FROM
            spesialutbetaling su
                JOIN vedtak v ON v.vedtak_id = su.vedtak_id -- for å få sak_id
        WHERE
            su.person_id = ?
            AND su.MOD_DATO >= ADD_MONTHS(TRUNC(SYSDATE), -60)
        
        UNION ALL
        
        SELECT
            v.sak_id,
            v.vedtakstatuskode,
            v.vedtakstatuskode,
            ssu.dato_periode_fra AS fra_dato,
            ssu.dato_periode_til AS til_dato,
            'SIM_SPESIAL' AS rettighetkode
        FROM
            sim_utbetalingsgrunnlag ssu
                JOIN vedtak v ON v.vedtak_id = ssu.vedtak_id
        WHERE
           ssu.person_id = ?
           AND ssu.mod_dato >= ADD_MONTHS(TRUNC(SYSDATE), -3) -- ignorer gamle simuleringer som ikke ble noe av
        """.trimIndent()

        // S1: Hent alle AAP-vedtak med relevant historikk for personen
        // OBS 1: tabellen i Prod har forekomster av at til_dato er før fra_dato.
        // De kalles for "ugyldiggjorte vedtak", og for "deaktiverte saker". Vi ekskluderer disse vedtakene her.
        // OBS 2: De samme feltene kan være (null, null). Dette er "etterregistrerte vedtak" som er opprettet i
        // forbindelse med spesialutbetaling for perioder hvor det allerede finnes et ytelsesvedtak i Arena, AAP,
        // dagpenger eller tiltakspenger. Vi ekskluderer også disse vedtakene her, ettersom det altså finnes et ordinært
        // vedtak i samme periode.
        @Language("OracleSql")
        val selectKunRelevanteVedtak = """
        SELECT sak_id, vedtakstatuskode, vedtaktypekode, fra_dato, til_dato, rettighetkode
          FROM 
              vedtak v 
        
        WHERE v.person_id = ?
          AND (v.utfallkode IS NULL OR v.utfallkode != 'AVBRUTT')
          AND v.rettighetkode IN ('AA115', 'AAP')
          AND v.MOD_DATO >= ADD_MONTHS(TRUNC(SYSDATE), -72) -- ytelse: unngå å løpe gjennom veldig gamle vedtak
          AND NOT (fra_dato > til_dato AND (til_dato IS NOT NULL AND fra_dato IS NOT NULL)) -- filtrer ut ugyldiggjorte vedtak
          AND ((fra_dato IS NOT NULL OR til_dato IS NOT NULL) OR vedtakstatuskode IN ('OPPRE', 'MOTAT', 'REGIS', 'INNST')) -- filtrer ut etterregistrerte vedtak, men behold vedtak som er under behandling
          AND ( 
                (vedtaktypekode IN ('O','E','G') AND (til_dato >= ? OR til_dato IS NULL)) -- vanlig tidsbuffer på 18 måneder
                  OR
                (vedtaktypekode = 'S' AND (fra_dato >= ? OR fra_dato IS NULL)) -- ekstra tidsbuffer for Stans, som bare har fra_dato
              )
          AND NOT (utfallkode = 'NEI' AND til_dato IS NULL AND fra_dato <= ?) -- utfallkode NEI vil ha åpen til_dato, så ekskluder disse når de er gamle 
        """.trimIndent()

        // S2: Hent alle AAP-klager med relevant historikk for personen
        // Forbedringsmulighet: Vi kan se bort i fra klag1 for de som har klag2, angitt ved vedtak.vedtak_id_relatert
        @Language("OracleSql")
        val selectKunRelevanteKlager = """
        -- INNVF er satt for alle klager. Den får alltid en dato-verdi når utfallet av klagen registreres. 
        -- Dersom den er null, er klagen fortsatt under behandling.
        SELECT
            v.sak_id,
            vedtakstatuskode,
            vedtaktypekode,
            CAST(NULL AS DATE)                    AS fra_dato,
            TO_DATE(vf.vedtakverdi, 'DD-MM-YYYY') AS til_dato,
            v.rettighetkode
        FROM
            vedtak v
            JOIN vedtakfakta vf ON vf.vedtak_id = v.vedtak_id
        WHERE
            v.person_id = ?
            AND (v.utfallkode IS NULL OR v.utfallkode != 'AVBRUTT')
            AND v.rettighetkode IN ( 'KLAG1', 'KLAG2' )
            AND v.MOD_DATO >= ADD_MONTHS(TRUNC(SYSDATE), -72) -- ytelse: unngå å løpe gjennom veldig gamle vedtak, begrens string-til-dato konvertering
            AND vf.vedtakfaktakode = 'INNVF'
            -- Vi regner klager med null INNVF som åpne. Klager med fersk INNVF-dato regnes også som åpne, pga. det tar tid før AAP-vedtakene registreres.  
            -- Og at det kan komme en ny klage eller anke etter at klagen er behandlet og avslått. Anker sjekkes for seg selv.
            AND ( vf.vedtakverdi IS NULL OR TO_DATE(vf.vedtakverdi, 'DD-MM-YYYY') >= ? )
            -- Dersom klagen ble innvilget for mer enn 6 mnd siden, regnes den som ikke relevant lenger. Ekskluder disse.
            AND NOT ( vf.vedtakverdi IS NOT NULL AND TO_DATE(vf.vedtakverdi, 'DD-MM-YYYY') <= ADD_MONTHS(TRUNC(SYSDATE), -6) AND v.utfallkode IN ('JA', 'DELVIS' ) )
        """.trimIndent()

        // S3: Hent alle AAP-anker med relevant historikk for personen
        @Language("OracleSql")
        val selectKunRelevanteAnker = """
        SELECT
            v.sak_id,
            vedtakstatuskode,
            vedtaktypekode,
            CAST(NULL AS DATE)                    AS fra_dato,
            CAST(NULL AS DATE)                    AS til_dato,
            v.rettighetkode
        FROM
            vedtak v
            JOIN vedtakfakta vf ON vf.vedtak_id = v.vedtak_id
        WHERE
            v.person_id = ?
            AND (v.utfallkode IS NULL OR v.utfallkode != 'AVBRUTT')
            AND rettighetkode = 'ANKE'
            AND v.MOD_DATO >= ADD_MONTHS(TRUNC(SYSDATE), -72) -- ytelse: unngå å løpe gjennom veldig gamle vedtak
        """.trimIndent()

        // S4: Hent alle tilbakebetalinger med relevant historikk for personen
        // MERK: denne spørringen går veldig tregt, av ukjent grunn
        @Language("OracleSql")
        val selectKunRelevanteTilbakebetalinger = """
        SELECT
            v.sak_id,
            vedtakstatuskode,
            vedtaktypekode,
            CAST(NULL AS DATE)                    AS fra_dato,
            TO_DATE(vf.vedtakverdi, 'DD-MM-YYYY') AS til_dato,
            v.rettighetkode
        FROM
            vedtak v
            JOIN vedtakfakta vf ON vf.vedtak_id = v.vedtak_id
        WHERE
            v.person_id = ?
            AND rettighetkode = 'TILBBET'
            AND (v.utfallkode IS NOT NULL AND v.utfallkode != 'AVBRUTT')
            AND v.MOD_DATO >= ADD_MONTHS(TRUNC(SYSDATE), -60) -- ytelse: unngå å løpe gjennom veldig gamle vedtak
            AND vf.vedtakfaktakode = 'INNVF'
            -- Vi regner tilbakebetalinger med null INNVF som åpne, ellers ikke 
            AND vf.vedtakverdi IS NULL -- det er ikke satt endelig dato for beslutning på vedtaket
            """.trimIndent()

        // S5: Hent alle spesialutbetalinger med relevant historikk for personen
        @Language("OracleSql")
        val selectKunRelevanteSpesialutbetalinger = """
        SELECT
            v.sak_id,
            su.vedtakstatuskode,
            'O'  AS vedtaktypekode, 
            su.dato_fra AS fra_dato,
            su.dato_til AS til_dato,
            'SPESIAL' AS rettighetkode
        FROM
            spesialutbetaling su
            JOIN vedtak v ON v.vedtak_id = su.vedtak_id -- for å få sak_id
        WHERE
            su.person_id = ?
            -- Dersom utbetalingen ikke er datofestet, eller den har skjedd nylig, regner vi saken som åpen, ellers ikke. 
            -- Vi bruker en tidsbuffer her i tilfelle det klages på spesialutbetalingen etter at den er utbetalt.
            AND (su.dato_utbetaling IS NULL OR su.dato_utbetaling >= ADD_MONTHS(TRUNC(SYSDATE), -3) )
            -- MERK: ingen index i spesialutbetaling på dato_utbetaling eller andre dato-felt, så det går tregt
        """.trimIndent()

        // S6: Hent simulerte betalinger.
        // Saksbehandler gjør noen ganger simuleringer før en reell betaling og vedtak opprettes.
        @Language("OracleSql")
        val selectKunRelevanteSimulerteBetalinger = """
        SELECT
            v.sak_id, 
            v.vedtakstatuskode, 
            v.vedtakstatuskode, 
            ssu.dato_periode_fra AS fra_dato,
            ssu.dato_periode_til AS til_dato,
            'SIM_SPESIAL' AS rettighetkode
        FROM
            sim_utbetalingsgrunnlag ssu
            JOIN vedtak v ON v.vedtak_id = ssu.vedtak_id
        WHERE 
            ssu.person_id = ?
            -- MERK: ingen index i sim_utbetalingsgrunnlag på mod_dato eller andre datofelt, så blir tregt
            AND ssu.mod_dato >= ADD_MONTHS(TRUNC(SYSDATE), -3) -- ignorer gamle simuleringer som ikke ble noe av
        """.trimIndent()

        const val tidsBufferUkerGenerell = 78L
        const val tidsBufferUkerStans = 119L // foreldrepenger 80% utbetalt, trillinger alenemor
        fun hentAlleSignifikanteVedtakForPerson(
            personId: Int, `søknadMottattPå`: LocalDate, connection: Connection
        ): List<ArenaVedtak> {
            val tidsBufferGenerell = søknadMottattPå.minusWeeks(tidsBufferUkerGenerell)
            val nyesteTillateStans = søknadMottattPå.minusWeeks(tidsBufferUkerStans)
            val query =
                listOf(
                    selectKunRelevanteVedtak,
                    selectKunRelevanteKlager,
                    selectKunRelevanteAnker,
                    selectKunRelevanteTilbakebetalinger,
                    selectKunRelevanteSpesialutbetalinger,
                    selectKunRelevanteSimulerteBetalinger
                ).joinToString("\nUNION ALL\n")

            connection.createParameterizedQuery(query).use { preparedStatement ->
                var p = 1 // parameter-indeks
                // vedtak
                preparedStatement.setInt(p++, personId)
                preparedStatement.setDate(p++, Date.valueOf(tidsBufferGenerell))
                preparedStatement.setDate(p++, Date.valueOf(nyesteTillateStans))
                preparedStatement.setDate(p++, Date.valueOf(tidsBufferGenerell))
                // klager
                preparedStatement.setInt(p++, personId)
                preparedStatement.setDate(p++, Date.valueOf(tidsBufferGenerell))
                // anker
                preparedStatement.setInt(p++, personId)
                // tilbakebetalinger
                preparedStatement.setInt(p++, personId)
                // spesialutbetalinger
                preparedStatement.setInt(p++, personId)
                // sim_utbetalingsgrunnlag
                preparedStatement.setInt(p++, personId)

                val resultSet = preparedStatement.executeQuery()
                return resultSet.map { row -> mapperForArenaVedtak(row) }
            }
        }

        fun mapperForArenaVedtak(row: ResultSet) = ArenaVedtak(
            row.getString("sak_id"),
            row.getString("vedtakstatuskode"),
            row.getString("vedtaktypekode"),
            fraDato(row.getDate("fra_dato")),
            tilDato = fraDato(row.getDate("til_dato")),
            rettighetkode = row.getString("rettighetkode"),
        )

    }

}
