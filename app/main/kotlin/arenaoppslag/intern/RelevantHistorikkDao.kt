package arenaoppslag.intern

import arenaoppslag.datasource.map
import arenaoppslag.intern.InternDao.mapperForArenasak
import no.nav.aap.arenaoppslag.kontrakt.intern.ArenaSak
import org.intellij.lang.annotations.Language
import org.jetbrains.annotations.TestOnly
import java.sql.Connection
import java.sql.Date
import java.time.LocalDate

object RelevantHistorikkDao {
    private const val FNR_LISTE_TOKEN = "?:fodselsnummer"

    // S1: Hent alle AAP-vedtak med relevant historikk for personen
    // OBS 1: tabellen i Prod har forekomster av at til_dato er før fra_dato.
    // De kalles for "ugyldiggjorte vedtak", og for "deaktiverte saker". Vi ekskluderer disse vedtakene her.
    // OBS 2: De samme feltene kan være (null, null). Dette er "etterregistrerte vedtak" som er opprettet i forbindelse med
    // spesialutbetaling for perioder hvor det allerede finnes et ytelsesvedtak i Arena, AAP, dagpenger eller tiltakspenger.
    // Vi ekskluderer også disse vedtakene her, ettersom det altså finnes et ordinært vedtak i samme periode.
    @Language("OracleSql")
    val selectKunAAPVedtakForPersonMedRelevantHistorikk = """
        SELECT sak_id, vedtakstatuskode, vedtaktypekode, fra_dato, til_dato, rettighetkode
          FROM 
              vedtak v 
              JOIN person p on p.person_id=v.person_id
        
        WHERE p.fodselsnr IN ($FNR_LISTE_TOKEN)
          AND v.utfallkode != 'AVBRUTT'
          AND v.rettighetkode IN ('AA115', 'AAP') 
          AND (fra_dato <= til_dato OR til_dato IS NULL) -- filtrer ut ugyldiggjorte vedtak, men inkluder vedtak med null til_dato, som Stans
          AND NOT (fra_dato IS NULL AND til_dato IS NULL) -- filtrer ut etterregistrerte vedtak
          AND ( 
                (vedtaktypekode IN ('O','E','G') AND (til_dato >= ? OR til_dato IS NULL)) -- vanlig tidsbuffer
                  OR
                (vedtaktypekode = 'S' AND (fra_dato >= ? OR fra_dato IS NULL)) -- ekstra tidsbuffer for Stans, som bare har fra_dato
              )
           AND NOT (utfallkode = 'NEI' AND til_dato IS NULL AND fra_dato >=?) -- utfallkode NEI vil ha åpen til_dato, så ekskluder disse når de er gamle 
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
        -- Og at det kan komme en ny klage eller anke etter at klagen er behandlet og avslått. 
        AND ( vf.vedtakverdi IS NULL OR TO_DATE(vf.vedtakverdi, 'DD-MM-YYYY') >= ? )
        -- Dersom klagen ble innvilget eller avlyst etc. (utfallkoder utenom 'NEI') for mer enn 6 mnd siden, regnes den som ikke relevant lenger.
        AND NOT (v.utfallkode != 'NEI' AND TO_DATE(vf.vedtakverdi, 'DD-MM-YYYY') <= ADD_MONTHS(TRUNC(SYSDATE), -6))
        -- TODO må verifisere bruk av v.utfallkode - den er kanskje tvilsom eller omvendt av forventet
        -- K_UTFALL har verdiene: TRUKK NEI null JA DELVIS OPPHEVET AVVIST
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
        AND NOT exists ( SELECT 1 FROM vedtakfakta vf_innvilget
          WHERE
              vf_innvilget.vedtak_id = v.vedtak_id
              AND vf_innvilget.vedtakfaktakode = 'KJENNELSE'
              AND vf_innvilget.vedtakverdi = 'JA' -- kanskje også DELVIS eller andre verdier?
              AND EXISTS ( SELECT 1 FROM vedtakfakta vf_innvilget_dt
                  WHERE
                      vf_innvilget_dt.vedtak_id = v.vedtak_id  -- det samme vedtaket
                      AND vf_innvilget_dt.vedtakfaktakode = 'KJREGDATO' -- sin registrerte dato
                      AND TO_DATE(vf_innvilget_dt.vedtakverdi, 'DD-MM-YYYY') <= add_months( trunc(sysdate), -6 ) -- er 6 mnd eller eldre
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
        AND (vf.vedtakverdi IS NULL)
    """.trimIndent()

    // S5: Hent alle spesialutbetalinger med relevant historikk for personen
    @Language("OracleSql")
    val selectKunSpesialutbetalingerForPersonMedRelevantHistorikk = """
    SELECT
        v.sak_id,
        vedtakstatuskode,
        vedtaktypekode,
        CAST(NULL AS DATE)                    AS fra_dato,
        TO_DATE(vf.vedtakverdi, 'DD-MM-YYYY') AS til_dato,
        'SPESIAL' AS rettighetkode
    FROM
        sim_utbetalingsgrunnlag fu
        JOIN vedtak v ON v.vedtak_id = fu.vedtak_id
        JOIN person p ON p.person_id = v.person_id
        JOIN vedtakfakta vf ON v.vedtak_id = vf.vedtak_id
    WHERE
        p.fodselsnr IN ($FNR_LISTE_TOKEN)
        AND v.utfallkode != 'AVBRUTT'
        AND vf.vedtakfaktakode = 'INNVF'
        -- Vi regner vedtak med null INNVF som åpne, ellers ikke
        AND (vf.vedtakverdi IS NULL)    
        """.trimIndent()


    @TestOnly
    internal val historiskeRettighetkoderIArena = setOf(
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

    private fun queryMedFodselsnummerListe(baseQuery: String, fodselsnummer: List<String>): String {
        // Oracle lar oss ikke bruke liste-parameter i prepared statements, så vi bygger inn fødselsnumrene direkte i spørringen her
        val allePersonensFodselsnummer = fodselsnummer.joinToString(separator = ",") { "'$it'" }
        return baseQuery.replace(FNR_LISTE_TOKEN, allePersonensFodselsnummer)
    }

    const val tidsBufferUkerGenerell = 78L
    const val tidsBufferUkerStans = 119L // foreldrepenger 80% utbetalt, trillinger alenemor
    fun selectPersonMedRelevantHistorikk(
        fodselsnummer: List<String>,
        søknadMottattPå: LocalDate,
        connection: Connection
    ): List<ArenaSak> {
        val tidsBufferGenerell = søknadMottattPå.minusWeeks(tidsBufferUkerGenerell)
        val nyesteTillateStans = søknadMottattPå.minusWeeks(tidsBufferUkerStans)
        val query = queryMedFodselsnummerListe(selectKunAAPVedtakForPersonMedRelevantHistorikk, fodselsnummer)
        connection.prepareStatement(query)
            .use { preparedStatement ->
                preparedStatement.setDate(1, Date.valueOf(tidsBufferGenerell))
                preparedStatement.setDate(2, Date.valueOf(nyesteTillateStans))
                preparedStatement.setDate(3, Date.valueOf(tidsBufferGenerell))
                val resultSet = preparedStatement.executeQuery()
                return resultSet.map { row -> mapperForArenasak(row) }
            }
    }
}