package arenaoppslag.database

import arenaoppslag.database.DbDato.fraDato
import no.nav.aap.arenaoppslag.kontrakt.intern.ArenaSak
import org.intellij.lang.annotations.Language
import org.jetbrains.annotations.TestOnly
import java.sql.Connection
import java.sql.Date
import java.sql.ResultSet
import java.time.LocalDate
import javax.sql.DataSource


class PersonRepository(private val dataSource: DataSource) {

    fun hentEksistererIAAPArena(fodselsnr: String): Boolean {
        return dataSource.connection.use { con ->
            selectPersonMedFnrEksisterer(fodselsnr, con)
        }
    }

    fun hentRelevanteArenaSaker(personIdentifikatorer: List<String>, søknadMottattPå: LocalDate): List<ArenaSak> {
        val relevanteArenaSaker = dataSource.connection.use { con ->
            selectPersonMedRelevantHistorikk(
                personIdentifikatorer, søknadMottattPå, con
            )
        }
        return relevanteArenaSaker
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

        private const val FNR_LISTE_TOKEN = "?:fodselsnummer"

        // S1: Hent alle AAP-vedtak med relevant historikk for personen
        // OBS 1: tabellen i Prod har forekomster av at til_dato er før fra_dato.
        // De kalles for "ugyldiggjorte vedtak", og for "deaktiverte saker". Vi ekskluderer disse vedtakene her.
        // OBS 2: De samme feltene kan være (null, null). Dette er "etterregistrerte vedtak" som er opprettet i
        // forbindelse med spesialutbetaling for perioder hvor det allerede finnes et ytelsesvedtak i Arena, AAP,
        // dagpenger eller tiltakspenger. Vi ekskluderer også disse vedtakene her, ettersom det altså finnes et ordinært
        // vedtak i samme periode.
        @Language("OracleSql")
        val hentRelevanteAAPVedtakForPerson = """
        SELECT sak_id, vedtakstatuskode, vedtaktypekode, fra_dato, til_dato, rettighetkode
          FROM 
              vedtak v 
              JOIN person p on p.person_id=v.person_id
        
        WHERE p.fodselsnr IN ($FNR_LISTE_TOKEN)
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
        val selectKunKlagerForPersonMedRelevantHistorikk = """
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
        JOIN vedtakfakta vf ON v.vedtak_id = vf.vedtak_id
        JOIN person      p ON p.person_id = v.person_id
    WHERE
        p.fodselsnr IN ($FNR_LISTE_TOKEN)
        AND v.utfallkode != 'AVBRUTT'
        AND v.rettighetkode IN ( 'KLAG1', 'KLAG2' )
        AND vf.vedtakfaktakode = 'INNVF'
        -- Vi regner klager med null INNVF som åpne. Klager med fersk INNVF-dato regnes også som åpne, pga. det tar tid før AAP-vedtakene registreres.  
        -- Og at det kan komme en ny klage eller anke etter at klagen er behandlet og avslått. Anker sjekkes for seg selv.
        AND ( vf.vedtakverdi IS NULL OR TO_DATE(vf.vedtakverdi, 'DD-MM-YYYY') >= ? )
        -- Dersom klagen ble innvilget for mer enn 6 mnd siden, regnes den som ikke relevant lenger.
        -- Vi bruker vedtakfaktakode=K_UTFALL fremfor utfallkode=JA her, fordi vi ser uventet utfallkode for noen innvilgede klager i produksjon.
        AND NOT EXISTS(SELECT 1 from vedtakfakta vf_innvilget WHERE vf_innvilget.vedtakfaktakode = 'K_UTFALL' 
            AND vf_innvilget.vedtak_id = v.vedtak_id -- samme vedtaket
            AND vf_innvilget.vedtakverdi = 'JA' -- er innvilget (kan evt utvides med flere verdier)
            AND TO_DATE(vf.vedtakverdi, 'DD-MM-YYYY') <= ADD_MONTHS(TRUNC(SYSDATE), -6) -- er minst 6 mnd siden
            )
    """.trimIndent()

        // S3: Hent alle AAP-anker med relevant historikk for personen
        @Language("OracleSql")
        val selectKunAnkerForPersonMedRelevantHistorikk = """
        SELECT
        v.sak_id,
        vedtakstatuskode,
        vedtaktypekode,
        CAST(NULL AS DATE)                    AS fra_dato,
        TO_DATE(vf.vedtakverdi, 'DD-MM-YYYY') AS til_dato,
        v.rettighetkode
    FROM
        vedtak v
        JOIN person      p ON p.person_id = v.person_id
        JOIN vedtakfakta vf ON v.vedtak_id = vf.vedtak_id
    WHERE
        p.fodselsnr IN ($FNR_LISTE_TOKEN)
        AND rettighetkode = 'ANKE'
        AND utfallkode != 'AVBRUTT'
        AND vf.vedtakfaktakode = 'KJREGDATO'
        AND ( vf.vedtakverdi IS NULL OR TO_DATE(vf.vedtakverdi, 'DD-MM-YYYY') >= ? ) -- stor tidsbuffer her, da det kan ankes oppover i rettsvesenet.
        -- Dersom anken ble innvilget for mer enn 6 mnd siden, regnes den som ikke relevant lenger.
        -- Vi bruker vedtakfaktakode=KJENNELSE fremfor vedtak.utfallkode=JA her, fordi vi ser uventet utfallkode for noen innvilgede anker i produksjon.
        AND NOT EXISTS(SELECT 1 from vedtakfakta vf_innvilget WHERE vf_innvilget.vedtakfaktakode = 'KJENNELSE' 
            AND vf_innvilget.vedtak_id = v.vedtak_id -- samme vedtaket
            AND vf_innvilget.vedtakverdi = 'JA' -- er innvilget (kan evt utvides med flere verdier)
            AND TO_DATE(vf.vedtakverdi, 'DD-MM-YYYY') <= ADD_MONTHS(TRUNC(SYSDATE), -6) -- er minst 6 mnd siden
            )
    """.trimIndent()

        // S4: Hent alle tilbakebetalinger med relevant historikk for personen
        @Language("OracleSql")
        val selectKunTilbakebetalingerForPersonMedRelevantHistorikk = """
    SELECT
        v.sak_id,
        vedtakstatuskode,
        vedtaktypekode,
        CAST(NULL AS DATE)                    AS fra_dato,
        TO_DATE(vf.vedtakverdi, 'DD-MM-YYYY') AS til_dato,
        v.rettighetkode
    FROM
        vedtak v
        JOIN person p ON p.person_id = v.person_id
        JOIN vedtakfakta vf ON v.vedtak_id = vf.vedtak_id
    WHERE
        p.fodselsnr IN ($FNR_LISTE_TOKEN)
        AND rettighetkode = 'TILBBET'
        AND utfallkode != 'AVBRUTT'
        AND vf.vedtakfaktakode = 'INNVF'
        -- Vi regner tilbakebetalinger med null INNVF som åpne, ellers ikke.    
        AND vf.vedtakverdi IS NULL
    """.trimIndent()

        // S5: Hent alle spesialutbetalinger med relevant historikk for personen
        @Language("OracleSql")
        val selectKunSpesialutbetalingerForPersonMedRelevantHistorikk = """
    SELECT
        v.sak_id,
        su.vedtakstatuskode,
        CAST(NULL AS VARCHAR2(10))  AS vedtaktypekode, 
        su.dato_fra,
        su.dato_til,
        'SPESIAL' AS rettighetkode
    FROM
        spesialutbetaling su
        JOIN vedtak v ON v.vedtak_id = su.vedtak_id -- for å få sak_id
        JOIN person p ON p.person_id = su.person_id
    WHERE
        p.fodselsnr IN ($FNR_LISTE_TOKEN)
        -- Dersom utbetalingen ikke er datofestet, eller den har skjedd nylig, regner vi saken som åpen, ellers ikke. 
        -- Vi bruker en tidsbuffer her i tilfelle det klages på spesialutbetalingen etter at den er utbetalt.
        AND (su.dato_utbetaling is null OR su.dato_utbetaling >= ADD_MONTHS(TRUNC(SYSDATE), -3) )
        """.trimIndent()


        private fun queryMedFodselsnummerListe(baseQuery: String, fodselsnummerene: List<String>): String {
            // Oracle lar oss ikke bruke liste-parameter i prepared statements, så vi bygger inn fødselsnumrene direkte
            // i spørringen her
            val allePersonensFodselsnummer = fodselsnummerene.joinToString(separator = ",") { "'$it'" }
            return baseQuery.replace(FNR_LISTE_TOKEN, allePersonensFodselsnummer)
        }

        const val tidsBufferUkerGenerell = 78L
        const val tidsBufferUkerStans = 119L // foreldrepenger 80% utbetalt, trillinger alenemor
        fun selectPersonMedRelevantHistorikk(
            fodselsnummerene: List<String>, søknadMottattPå: LocalDate, connection: Connection
        ): List<ArenaSak> {
            val tidsBufferGenerell = søknadMottattPå.minusWeeks(tidsBufferUkerGenerell)
            val nyesteTillateStans = søknadMottattPå.minusWeeks(tidsBufferUkerStans)
            val query = queryMedFodselsnummerListe(hentRelevanteAAPVedtakForPerson, fodselsnummerene)
            connection.prepareStatement(query).use { preparedStatement ->
                preparedStatement.setDate(1, Date.valueOf(tidsBufferGenerell))
                preparedStatement.setDate(2, Date.valueOf(nyesteTillateStans))
                preparedStatement.setDate(3, Date.valueOf(tidsBufferGenerell))
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


        @Language("OracleSql")
        private val selectPersonMedFnrEksisterer = """
        SELECT * 
        FROM person 
        WHERE fodselsnr = ?
    """.trimIndent()

        fun selectPersonMedFnrEksisterer(
            fodselsnr: String, connection: Connection
        ): Boolean {
            return connection.prepareStatement(selectPersonMedFnrEksisterer).use { preparedStatement ->
                preparedStatement.setString(1, fodselsnr)
                val resultSet = preparedStatement.executeQuery()
                resultSet.next()
            }
        }


    }

}
