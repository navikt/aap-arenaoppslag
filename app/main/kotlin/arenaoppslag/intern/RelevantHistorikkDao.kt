package arenaoppslag.intern

import arenaoppslag.datasource.map
import arenaoppslag.intern.InternDao.mapperForArenasak
import no.nav.aap.arenaoppslag.kontrakt.intern.ArenaSak
import org.intellij.lang.annotations.Language
import org.jetbrains.annotations.TestOnly
import java.sql.Connection
import java.sql.Date
import java.time.LocalDate
import kotlin.use

object RelevantHistorikkDao {
    private const val FNR_LISTE_TOKEN = "?:fodselsnummer"

    // OBS 1: tabellen i Prod har forekomster av at til_dato er før fra_dato.
    // De kalles for "ugyldiggjorte vedtak", og for "deaktiverte saker". Vi ekskluderer disse vedtakene her.
    // OBS 2: De samme feltene kan være (null, null). Dette er "etterregistrerte vedtak" som er opprettet i forbindelse med
    // spesialutbetaling for perioder hvor det allerede finnes et ytelsesvedtak i Arena, AAP, dagpenger eller tiltakspenger.
    // Vi ekskluderer også disse vedtakene her, ettersom det altså finnes et ordinært vedtak i samme periode.
    @Language("OracleSql")
    val selectKunAAPVedtakForPersonMedRelevantHistorikk = """
        SELECT sak_id, vedtakstatuskode, vedtaktypekode, fra_dato, til_dato, rettighetkode
          FROM vedtak v JOIN person p on p.person_id=v.person_id
        
        WHERE p.fodselsnr IN ($FNR_LISTE_TOKEN)
          AND rettighetkode IN ('AA115', 'AAP') 
          AND (fra_dato <= til_dato OR til_dato IS NULL) -- filtrer ut ugyldiggjorte vedtak, men inkluder vedtak med null til_dato, som Stans
          AND NOT (fra_dato IS NULL AND til_dato IS NULL) -- filtrer ut etterregistrerte vedtak
          AND ( 
                (vedtaktypekode !='S' AND (til_dato >= ? OR til_dato IS NULL)) -- vanlig tidsbuffer
                  OR
                (vedtaktypekode = 'S' AND (fra_dato >= ? OR fra_dato IS NULL)) -- ekstra tidsbuffer for stans
              )
    """.trimIndent()

    @Language("OracleSql")
    val selectKunKlagerForPersonMedRelevantHistorikk = """
    -- INNVF er satt for alle klager. Den får alltid en dato-verdi når utfallet av klagen registreres?
    -- Dersom den er null, er klagen fortsatt under behandling.
    SELECT
        v.sak_id,
        vedtakstatuskode,
        vedtaktypekode,
        NULL                                  AS fra_dato,
        TO_DATE(vf.vedtakverdi, 'DD-MM-YYYY') AS til_dato,
        v.rettighetkode
    FROM
        vedtak v
        JOIN vedtakfakta vf ON v.vedtak_id = vf.vedtak_id
        JOIN person      p ON p.person_id = v.person_id
    WHERE
        p.fodselsnr IN ($FNR_LISTE_TOKEN)
        AND v.rettighetkode IN ( 'KLAG1', 'KLAG2' )
        AND vf.vedtakfaktakode = 'INNVF'
        -- Vi regner klager med null INNVF som åpne. Klager med fersk INNVF-dato regnes også som åpne, pga. det tar tid før AAP-vedtakene registreres.  
        -- Og at det kan komme en ny klage eller anke etter at klagen er behandlet og avslått. 
        AND ( vf.vedtakverdi IS NULL OR TO_DATE(vf.vedtakverdi, 'DD-MM-YYYY') >= ? )
        AND v.utfallkode NOT IN ('JA') -- dersom klagen er innvilget regnes den her som ikke relevant. Kan evt legge til TRUKK og andre koder her senere. 
    """.trimIndent()

    @Language("OracleSql")
    val selectKunAnkerForPersonMedRelevantHistorikk = """
    SELECT
        sak_id,
        vedtakstatuskode,
        vedtaktypekode,
        fra_dato,
        til_dato,
        rettighetkode
    FROM
        vedtak v
        JOIN person p ON p.person_id = v.person_id
    WHERE
        p.fodselsnr IN ( $FNR_LISTE_TOKEN )
        AND rettighetkode IN ( 'ANKE' )
        AND ( fra_dato >= ? OR fra_dato IS NULL ) -- tilbakebetaling har bare fra_dato       
    """.trimIndent()

    @Language("OracleSql")
    val selectKunTilbakebetalingerForPersonMedRelevantHistorikk = """
    SELECT
        sak_id,
        vedtakstatuskode,
        vedtaktypekode,
        fra_dato,
        til_dato,
        rettighetkode
    FROM
        vedtak v
        JOIN person p ON p.person_id = v.person_id
    WHERE
        p.fodselsnr IN ($FNR_LISTE_TOKEN)
        AND rettighetkode IN ( 'TILBBET' )
        AND ( fra_dato >= ? OR fra_dato IS NULL ) -- tilbakebetaling har bare fra_dato           
    """.trimIndent()

    @Language("OracleSql")
    val selectKunSpesialutbetalingerForPersonMedRelevantHistorikk = """
    -- spesialutbetalinger har nyeste dato i fra_dato-feltet, så vi bytter dem om her
    SELECT
        v.sak_id,
        vedtakstatuskode,
        vedtaktypekode,
        v.til_dato AS fra_dato,
        v.fra_dato AS til_dato,
        'SPESIAL' AS rettighetkode
    FROM
        sim_utbetalingsgrunnlag fu
        JOIN vedtak v ON v.vedtak_id = fu.vedtak_id
        JOIN person p ON p.person_id = v.person_id
    WHERE
        p.fodselsnr IN ( $FNR_LISTE_TOKEN )
        AND ( v.til_dato >= ? OR v.til_dato IS NULL ) -- til og fra byttet om
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
                val resultSet = preparedStatement.executeQuery()
                return resultSet.map { row -> mapperForArenasak(row) }
            }
    }
}