package no.nav.aap.arenaoppslag.database

import no.nav.aap.arenaoppslag.database.DbDato.fraDato
import no.nav.aap.arenaoppslag.kontrakt.intern.ArenaSak
import org.intellij.lang.annotations.Language
import org.jetbrains.annotations.TestOnly
import java.sql.Connection
import java.sql.Date
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.time.LocalDate
import javax.sql.DataSource
import kotlin.time.Duration.Companion.minutes

class HistorikkRepository(private val dataSource: DataSource) {

    fun hentAlleSignifikanteSakerForPerson(
        personId: Int, `søknadMottattPå`: LocalDate
    ): List<ArenaSak> {
        return dataSource.connection.use { con ->
            hentAlleSignifikanteSakerForPerson(personId, søknadMottattPå, con)
        }
    }

    companion object {
        @TestOnly
        val historiskeRettighetkoderIArena = setOf(
            // Alle disse har kun rettighetsperioder utløpt før 1/1/2022
            "AA116", // Behov for bistand
            "ABOUT", // Boutgifter
            "ATIO", // Tilsyn - barn over 10 år
            "ATIU", // Tilsyn - barn under 10 år
            "AHJMR", // Hjemreise
            "ATIF", // Tilsyn - familiemedlemmer
            "AFLYT", // Flytting
            "AATFOR", // Tvungen forvaltning
            "AUNDM" // Bøker og undervisningsmatriell
        )

        private fun Connection.createParameterizedQuery(queryString: String): PreparedStatement {
            val query = prepareStatement(queryString)
            query.queryTimeout = 600 // set a timeout in seconds, to avoid long running queries
            return query
        }

        // TODO kanskje skal vi først slå opp person_id for disse fnr-ene,
        //  og så bruke person_id i stedet for fnr i de andre spørringene?
        //  og cache person-id lokalt i ArenaService?

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
          AND v.utfallkode != 'AVBRUTT'
          AND v.rettighetkode IN ('AA115', 'AAP') 
          AND NOT (fra_dato > til_dato AND (til_dato IS NOT NULL AND fra_dato IS NOT NULL)) -- filtrer ut ugyldiggjorte vedtak
          AND NOT ((fra_dato IS NULL AND til_dato IS NULL) AND vedtakstatuskode NOT IN ('OPPRE', 'MOTAT', 'REGIS', 'INNST')) -- filtrer ut etterregistrerte vedtak, men behold vedtak som er under behandling
          AND ( 
                (vedtaktypekode IN ('O','E','G') AND (til_dato >= ? OR til_dato IS NULL)) -- vanlig tidsbuffer
                  OR
                (vedtaktypekode = 'S' AND (fra_dato >= ? OR fra_dato IS NULL)) -- ekstra tidsbuffer for Stans, som bare har fra_dato
              )
          AND NOT (utfallkode = 'NEI' AND til_dato IS NULL AND fra_dato <?) -- utfallkode NEI vil ha åpen til_dato, så ekskluder disse når de er gamle 
        """.trimIndent()

        // S2: Hent alle AAP-klager med relevant historikk for personen
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
            AND v.utfallkode != 'AVBRUTT'
            AND v.rettighetkode IN ( 'KLAG1', 'KLAG2' )
            AND v.MOD_DATO >= ADD_MONTHS(TRUNC(SYSDATE), -60) -- ytelse: unngå string-til-dato konvertering for veldig gamle rader
            AND vf.vedtakfaktakode = 'INNVF'
            -- Vi regner klager med null INNVF som åpne. Klager med fersk INNVF-dato regnes også som åpne, pga. det tar tid før AAP-vedtakene registreres.  
            -- Og at det kan komme en ny klage eller anke etter at klagen er behandlet og avslått. Anker sjekkes for seg selv.
            AND ( vf.vedtakverdi IS NULL OR TO_DATE(vf.vedtakverdi, 'DD-MM-YYYY') >= ? )
            -- Dersom klagen ble innvilget for mer enn 6 mnd siden, regnes den som ikke relevant lenger.
            -- Vi bruker vedtakfaktakode=K_UTFALL fremfor utfallkode=JA her, fordi vi ser uventet utfallkode for noen innvilgede klager i produksjon.
            AND NOT EXISTS(SELECT 1 from vedtakfakta vf_innvilget 
                WHERE vf_innvilget.vedtak_id = v.vedtak_id -- samme vedtaket 
                    AND vf_innvilget.vedtakfaktakode = 'K_UTFALL'
                    AND vf_innvilget.vedtakverdi = 'JA' -- er innvilget (kan evt utvides med flere verdier)
                    AND TO_DATE(vf.vedtakverdi, 'DD-MM-YYYY') <= ADD_MONTHS(TRUNC(SYSDATE), -6) -- er minst 6 mnd siden
                )
        """.trimIndent()

        // S3: Hent alle AAP-anker med relevant historikk for personen
        @Language("OracleSql")
        val selectKunRelevanteAnker = """
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
            AND rettighetkode = 'ANKE'
            AND utfallkode != 'AVBRUTT'
            AND v.MOD_DATO >= ADD_MONTHS(TRUNC(SYSDATE), -72) -- ytelse: unngå string-til-dato konvertering for veldig gamle rader
            AND vf.vedtakfaktakode = 'KJREGDATO'
            AND ( vf.vedtakverdi IS NULL OR TO_DATE(vf.vedtakverdi, 'DD-MM-YYYY') >= ADD_MONTHS(TRUNC(SYSDATE), -36) ) -- stor tidsbuffer her, da det kan ankes oppover i rettsvesenet.
            -- Dersom anken ble innvilget for mer enn 6 mnd siden, regnes den som ikke relevant lenger.
            -- Vi bruker vedtakfaktakode=KJENNELSE fremfor vedtak.utfallkode=JA her, fordi vi ser uventet utfallkode for noen innvilgede anker i produksjon.
            AND NOT EXISTS(SELECT 1 from vedtakfakta vf_innvilget 
                WHERE vf_innvilget.vedtak_id = v.vedtak_id -- samme vedtaket 
                    AND vf_innvilget.vedtakfaktakode = 'KJENNELSE'
                    AND vf_innvilget.vedtakverdi = 'JA' -- er innvilget (kan evt utvides med flere verdier)
                    AND TO_DATE(vf.vedtakverdi, 'DD-MM-YYYY') <= ADD_MONTHS(TRUNC(SYSDATE), -6) -- er minst 6 mnd siden
                )
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
            AND utfallkode != 'AVBRUTT'            
            AND vf.vedtakfaktakode = 'INNVF'
            -- Vi regner tilbakebetalinger med null INNVF som åpne, ellers ikke.    
            AND vf.vedtakverdi IS NULL
        """.trimIndent()

        // TODO kan kanskje droppe å joine med vedtak for å få sak_id, men heller ta det som et query etterpå,
        // for å hente sak_id for alle vedtak_id vi får her?
        // S5: Hent alle spesialutbetalinger med relevant historikk for personen
        @Language("OracleSql")
        val selectKunRelevanteSpesialutbetalinger = """
        SELECT
            v.sak_id,
            su.vedtakstatuskode,
            CAST(NULL AS VARCHAR2(10))  AS vedtaktypekode, 
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
            -- MERK: ingen index i spesialutbetaling på dato_utbetaling eller andre dato-felt, så blir tregt

        """.trimIndent()

        // S6: Hent uferdige spesialutbetalinger for personen, hvor kun simulering av utbetaling er gjort
        @Language("OracleSql")
        val selectKunRelevanteUferdigeSpesialutbetalinger = """
        SELECT
            v.sak_id, 
            v.vedtakstatuskode, 
            v.vedtakstatuskode, 
            ssu.dato_periode_fra AS fra_dato,
            ssu.dato_periode_til AS til_dato,
            'SIM_SPESIAL' AS rettighetkode
        FROM
            sim_utbetalingsgrunnlag ssu
            LEFT JOIN spesialutbetaling su ON su.person_id = ssu.person_id
            JOIN vedtak v ON v.vedtak_id = ssu.vedtak_id
        WHERE 
            ssu.person_id = ?
            -- MERK: ingen index i sim_utbetalingsgrunnlag på mod_dato eller andre felt, så blir tregt
            AND su.person_id IS NULL -- personen finnes ikke enda i SPESIALUTBETALINGER, og kommer kanskje senere 
            AND ssu.mod_dato >= ADD_MONTHS(TRUNC(SYSDATE), -3) -- ignorer gamle simuleringer som ikke ble noe av
        """.trimIndent()


        const val tidsBufferUkerGenerell = 78L
        const val tidsBufferUkerStans = 119L // foreldrepenger 80% utbetalt, trillinger alenemor
        fun hentAlleSignifikanteSakerForPerson(
            personId: Int, `søknadMottattPå`: LocalDate, connection: Connection
        ): List<ArenaSak> {
            val tidsBufferGenerell = søknadMottattPå.minusWeeks(tidsBufferUkerGenerell)
            val nyesteTillateStans = søknadMottattPå.minusWeeks(tidsBufferUkerStans)
            val query =
                listOf(
                    selectKunRelevanteVedtak,
                    selectKunRelevanteKlager,
                    selectKunRelevanteAnker,
                    selectKunRelevanteTilbakebetalinger,
                    selectKunRelevanteSpesialutbetalinger,
                    selectKunRelevanteUferdigeSpesialutbetalinger
                ).joinToString("\nUNION ALL\n")

            connection.createParameterizedQuery(query).apply {
                queryTimeout = 15.minutes.inWholeSeconds.toInt()
            }.use { preparedStatement ->
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
                return resultSet.map { row -> mapperForArenasak(row) }
            }
        }

        fun mapperForArenasak(row: ResultSet): ArenaSak = ArenaSak(
            row.getString("sak_id"),
            row.getString("vedtakstatuskode"),
            row.getString("vedtaktypekode"),
            fraDato(row.getDate("fra_dato")),
            tilDato = fraDato(row.getDate("til_dato")),
            rettighetkode = row.getString("rettighetkode")
        )

    }

}
