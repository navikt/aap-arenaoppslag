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

    // OBS 1: tabellen i Prod har forekomster av at til_dato er før fra_dato.
    // De kalles for "ugyldiggjorte vedtak", og for "deaktiverte saker". Vi ekskluderer disse vedtakene her.
    // OBS 2: De samme feltene kan være (null, null). Dette er "etterregistrerte vedtak" som er opprettet i forbindelse med
    // spesialutbetaling for perioder hvor det allerede finnes et ytelsesvedtak i Arena, AAP, dagpenger eller tiltakspenger.
    // Vi ekskluderer også disse vedtakene her, ettersom det altså finnes et gyldig vedtak i samme periode.
    @Language("OracleSql")
    val selectKunAAPVedtakForPersonMedRelevantHistorikk = """
        SELECT sak_id, vedtakstatuskode, vedtaktypekode, fra_dato, til_dato, rettighetkode
          FROM vedtak
        WHERE person_id = 
               (SELECT person_id 
                  FROM person 
                 WHERE fodselsnr = ?) 
              AND 
               rettighetkode IN ('AA115', 'AAP') 
               AND (fra_dato <= til_dato OR til_dato IS NULL) -- filtrer ut ugyldiggjorte vedtak, men inkluder stans med null til_dato
               AND NOT (fra_dato IS NULL AND til_dato IS NULL) -- filtrer ut etterregistrerte vedtak
               AND ( 
                    (vedtaktypekode !='S' AND (til_dato >= ? OR til_dato IS NULL)) -- vanlig tidsbuffer
                        OR
                    (vedtaktypekode = 'S' AND (til_dato >= ? OR til_dato IS NULL)) -- ekstra tidsbuffer for stans
                    )
               AND NOT (til_dato IS NULL AND fra_dato >= DATE '2020-01-01') -- filtrer ut åpen slutt kun hvis veldig gammel
    """.trimIndent()

    // Disse vedtakstatuskodene forekommer på klager og er datoer:  K_INNVF K_TDATO KLAGEFRIST K_FDATO
    @Language("OracleSql")
    val selectKunKlagerForPersonMedRelevantHistorikk = """
        -- K_TDATO settes kun når klagen er avsluttet?
        SELECT v.sak_id, vedtakstatuskode, vedtaktypekode, null as fra_dato, TO_DATE(vf.vedtakverdi, 'DD-MM-YYYY') AS til_dato, 
        rettighetkode,
          FROM vedtak v
          join vedtakfakta vf on v.vedtak_id = vf.vedtak_id
        WHERE v.person_id = 
               (SELECT person_id 
                  FROM person 
                 WHERE fodselsnr = ?) 
                
           AND rettighetkode IN ('KLAG1', 'KLAG2')
           AND vf.vedtakfaktakode = 'K_TDATO' 
           AND ( TO_DATE(vf.vedtakverdi, 'DD-MM-YYYY') >= ? OR vf.vedtakverdi is null )  
    """.trimIndent()

    @Language("OracleSql")
    val selectKunTilbakebetalingerForPersonMedRelevantHistorikk = """
        SELECT v.sak_id, vedtakstatuskode, vedtaktypekode, fra_dato, til_dato, rettighetkode
          FROM vedtak v
        WHERE v.person_id = 
               (SELECT person_id 
                  FROM person 
                 WHERE fodselsnr = ?) 
                 
        AND rettighetkode IN ('TILBBET')
        AND (fra_dato >= ? or fra_dato IS NULL) -- tilbakebetaling har bare fra_dato           
    """.trimIndent()

    @Language("OracleSql")
    val selectKunSpesialutbetalingerForPersonMedRelevantHistorikk = """
        -- spesialutbetalinger har nyeste dato i fra_dato-feltet, så vi bytter dem om her
        SELECT v.sak_id, vedtakstatuskode, vedtaktypekode, v.til_dato as fra_dato, v.fra_dato as til_dato, 'SPESIAL' as rettighetkode
          from sim_utbetalingsgrunnlag fu join vedtak v on v.vedtak_id=fu.vedtak_id 
        WHERE v.person_id = 
               (SELECT person_id 
                  FROM person 
                 WHERE fodselsnr = ?) 
                 
        AND (v.til_dato >= ? or v.til_dato IS NULL) -- til og fra byttet om
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

    const val tidsBufferUkerGenerell = 78L
    const val tidsBufferUkerStans = 119L // foreldrepenger 80% utbetalt, trillinger alenemor
    fun selectPersonMedRelevantHistorikk(
        personidentifikator: String,
        søknadMottattPå: LocalDate,
        connection: Connection
    ): List<ArenaSak> {
        val tidsBufferGenerell = søknadMottattPå.minusWeeks(tidsBufferUkerGenerell)
        val nyesteTillateStans = søknadMottattPå.minusWeeks(tidsBufferUkerStans)
        connection.prepareStatement(selectKunAAPVedtakForPersonMedRelevantHistorikk)
            .use { preparedStatement ->
                preparedStatement.setString(1, personidentifikator)
                preparedStatement.setDate(2, Date.valueOf(tidsBufferGenerell))
                preparedStatement.setDate(3, Date.valueOf(nyesteTillateStans))
                val resultSet = preparedStatement.executeQuery()
                return resultSet.map { row -> mapperForArenasak(row) }
            }
    }
}