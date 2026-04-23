package no.nav.aap.arenaoppslag.database

import no.nav.aap.arenaoppslag.database.DbDato.fraDato
import no.nav.aap.arenaoppslag.modeller.ArenaVedtak
import org.intellij.lang.annotations.Language
import java.sql.Connection
import java.sql.Date
import java.sql.ResultSet
import java.time.LocalDate
import javax.sql.DataSource

class HistorikkRepository(private val dataSource: DataSource) {

    fun hentAlleSignifikanteVedtakForPerson(
        arenaPersonId: Int, søknadMottattPå: LocalDate, nåDato: LocalDate = LocalDate.now()
    ): List<ArenaVedtak> {
        return dataSource.connection.use { con ->
            hentAlleSignifikanteVedtakForPerson(arenaPersonId, søknadMottattPå, nåDato, con)
        }
    }

    companion object {

        // S1: Hent alle AAP-vedtak med relevant historikk for personen
        // OBS 1: tabellen i Prod har forekomster av at til_dato er før fra_dato.
        // De kalles for "ugyldiggjorte vedtak", og for "deaktiverte saker". Vi ekskluderer disse vedtakene her.
        // OBS 2: De samme feltene kan være (null, null). Dette er "etterregistrerte vedtak" som er opprettet i
        // forbindelse med spesialutbetaling for perioder hvor det allerede finnes et ytelsesvedtak i Arena, AAP,
        // dagpenger eller tiltakspenger. Vi ekskluderer også disse vedtakene her, ettersom det altså finnes et ordinært
        // vedtak i samme periode.
        @Language("OracleSql")
        val selectKunRelevanteVedtak = """
        SELECT 
            sak_id, 
            vedtakstatuskode, 
            vedtaktypekode, 
            fra_dato, 
            til_dato, 
            rettighetkode, 
            utfallkode, 
            reg_dato
        FROM 
              vedtak v 
        WHERE v.person_id = ?
          AND (v.utfallkode IS NULL OR v.utfallkode != 'AVBRUTT')
          AND v.rettighetkode IN ('AA115', 'AAP')
          AND v.MOD_DATO >= ? -- ytelse: unngå å løpe gjennom veldig gamle vedtak
          AND NOT (fra_dato > til_dato AND (til_dato IS NOT NULL AND fra_dato IS NOT NULL)) -- filtrer ut ugyldiggjorte vedtak
          AND ((fra_dato IS NOT NULL OR til_dato IS NOT NULL) OR vedtakstatuskode IN ('OPPRE', 'MOTAT', 'REGIS', 'INNST')) -- filtrer ut etterregistrerte vedtak, men behold vedtak som er under behandling
          AND ( 
                (vedtaktypekode IN ('O','E','G') AND (til_dato IS NULL OR til_dato >= ?)) -- vanlig tidsbuffer
                  OR
                (vedtaktypekode = 'S' AND (fra_dato IS NULL OR fra_dato >= ?)) -- ekstra tidsbuffer for Stans, som bare har fra_dato
              )
          AND NOT (utfallkode = 'NEI' AND til_dato IS NULL AND rettighetkode='AAP' AND fra_dato <= ?) -- utfallkode NEI vil ha åpen til_dato, så ekskluder disse når de er gamle 
          AND NOT (utfallkode = 'NEI' AND til_dato IS NULL AND rettighetkode='AA115') -- bruker fikk avslag
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
            v.rettighetkode,
            v.utfallkode, 
            v.reg_dato
        FROM
            vedtak v
            JOIN vedtakfakta vf ON vf.vedtak_id = v.vedtak_id
        WHERE
            v.person_id = ?
            AND (v.utfallkode IS NULL OR v.utfallkode != 'AVBRUTT')
            AND v.rettighetkode IN ( 'KLAG1', 'KLAG2' )
            AND v.MOD_DATO >= ? -- ytelse: unngå å løpe gjennom veldig gamle vedtak
            AND vf.vedtakfaktakode = 'INNVF'
            -- Vi regner klager med null INNVF som åpne. Klager med fersk INNVF-dato regnes også som åpne, pga. det tar tid før AAP-vedtakene registreres.  
            -- Og at det kan komme en ny klage eller anke etter at klagen er behandlet og avslått. Anker sjekkes for seg selv.
            AND ( vf.vedtakverdi IS NULL OR TO_DATE(vf.vedtakverdi, 'DD-MM-YYYY') >= ? )
            -- Dersom klagen ble innvilget for lenge nok siden, regnes den som ikke relevant lenger. Ekskluder disse.
            AND NOT ( vf.vedtakverdi IS NOT NULL AND TO_DATE(vf.vedtakverdi, 'DD-MM-YYYY') <= ? AND v.utfallkode IN ('JA', 'DELVIS' ) )
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
            v.rettighetkode,
            v.utfallkode, 
            v.reg_dato
        FROM
            vedtak v
            JOIN vedtakfakta vf ON vf.vedtak_id = v.vedtak_id
        WHERE
            v.person_id = ?
            AND (v.utfallkode IS NULL OR v.utfallkode != 'AVBRUTT')
            AND rettighetkode = 'ANKE'
            AND v.MOD_DATO >= ? -- ytelse: unngå å løpe gjennom veldig gamle vedtak
        """.trimIndent()

        const val tidsBufferUkerGenerell = 78L // 52 uker + 6 måneder tilbakejustering
        const val tidsBufferUkerStans = 119L // foreldrepenger med 80% utbetalt, trillinger, alenemor
        const val modnedGrenseVedtak = 64L
        const val modnedGrenseKlageInnvilget = 6L

        fun hentAlleSignifikanteVedtakForPerson(
            arenaPersonId: Int, søknadMottattPå: LocalDate, nåDato: LocalDate, connection: Connection
        ): List<ArenaVedtak> {
            val tidsBufferGenerell = søknadMottattPå.minusWeeks(tidsBufferUkerGenerell)
            val nyesteTillateStans = søknadMottattPå.minusWeeks(tidsBufferUkerStans)
            val vedtakModnedGrense = Date.valueOf(nåDato.minusMonths(modnedGrenseVedtak))
            val klageInnvilgetGrense = Date.valueOf(nåDato.minusMonths(modnedGrenseKlageInnvilget))

            val query =
                listOf(
                    selectKunRelevanteVedtak,
                    selectKunRelevanteKlager,
                    selectKunRelevanteAnker,
                ).joinToString("\nUNION ALL\n")

            connection.createParameterizedQuery(query).use { preparedStatement ->
                var p = 1 // parameter-indeks
                // S1: vedtak
                preparedStatement.setInt(p++, arenaPersonId)
                preparedStatement.setDate(p++, vedtakModnedGrense)
                preparedStatement.setDate(p++, Date.valueOf(tidsBufferGenerell))
                preparedStatement.setDate(p++, Date.valueOf(nyesteTillateStans))
                preparedStatement.setDate(p++, Date.valueOf(tidsBufferGenerell))
                // S2: klager
                preparedStatement.setInt(p++, arenaPersonId)
                preparedStatement.setDate(p++, vedtakModnedGrense)
                preparedStatement.setDate(p++, Date.valueOf(tidsBufferGenerell))
                preparedStatement.setDate(p++, klageInnvilgetGrense)
                // S3: anker
                preparedStatement.setInt(p++, arenaPersonId)
                preparedStatement.setDate(p++, vedtakModnedGrense)

                val resultSet = preparedStatement.executeQuery()
                return resultSet.map { row -> mapperForArenaVedtak(row) }
            }
        }

        fun mapperForArenaVedtak(row: ResultSet) = ArenaVedtak(
            sakId = row.getString("sak_id"),
            statusKode = row.getString("vedtakstatuskode"),
            vedtaktypeKode = row.getString("vedtaktypekode"),
            fraOgMed = fraDato(row.getDate("fra_dato")),
            tilDato = fraDato(row.getDate("til_dato")),
            rettighetkode = row.getString("rettighetkode"),
            utfallkode = row.getString("utfallkode"),
            registrertDato = fraDato(row.getDate("reg_dato"))
        )

    }

}
