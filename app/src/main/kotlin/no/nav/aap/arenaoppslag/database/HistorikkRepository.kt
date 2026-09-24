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

        // S1: Hent alle AAP-vedtak med relevant historikk for personen
        // OBS 1: tabellen i Prod har forekomster av at til_dato er før fra_dato.
        // De kalles for "ugyldiggjorte vedtak", og for "deaktiverte saker". Vi ekskluderer disse vedtakene her.
        // OBS 2: De samme feltene kan være (null, null). Dette er "etterregistrerte vedtak" som er opprettet i
        // forbindelse med spesialutbetaling for perioder hvor det allerede finnes et ytelsesvedtak i Arena, AAP,
        // dagpenger eller tiltakspenger. Vi ekskluderer også disse vedtakene her, ettersom det altså finnes et ordinært
        // vedtak i samme periode.
        @Language("OracleSql")
        val selectKunRelevanteAapVedtak = """
        SELECT 
            sak_id, 
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
        FROM 
              vedtak v 
        WHERE v.person_id = ?
          AND (v.utfallkode IS NULL OR v.utfallkode != 'AVBRUTT')
          AND v.rettighetkode = 'AAP'
          AND v.MOD_DATO >= ? -- ytelse: unngå å løpe gjennom veldig gamle vedtak
          AND NOT (fra_dato > til_dato AND (til_dato IS NOT NULL AND fra_dato IS NOT NULL)) -- filtrer ut ugyldiggjorte vedtak
          AND ((fra_dato IS NOT NULL OR til_dato IS NOT NULL) OR vedtakstatuskode IN ('OPPRE', 'MOTAT', 'REGIS', 'INNST')) -- filtrer ut etterregistrerte vedtak, men behold vedtak som er under behandling
          AND ( 
                ((vedtaktypekode IN ('O','E','G') OR (vedtaktypekode = 'S' and v.til_dato IS NOT NULL)) AND (til_dato IS NULL OR til_dato >= ?)) -- vanlig tidsbuffer
                  OR
                (vedtaktypekode = 'S' AND til_dato IS NULL AND (fra_dato IS NULL OR fra_dato >= ?)) -- ekstra tidsbuffer for Stans, som bare har fra_dato
              )
          AND NOT (utfallkode = 'NEI' AND til_dato IS NULL AND (fra_dato IS NOT NULL AND fra_dato <= ?)) -- utfallkode NEI vil ha åpen til_dato, så ekskluder disse når de er gamle
        """.trimIndent()


        // S2: Hent alle AA115-vedtak med relevant historikk for personen
        @Language("OracleSql")
        val selectKunRelevante11_5Vedtak = """
        SELECT 
            sak_id, 
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
        FROM 
              vedtak v 
        WHERE v.person_id = ?
          AND v.rettighetkode = 'AA115'
          AND v.utfallkode IS NULL -- ikke behandlet enda 
          AND v.MOD_DATO >= ? -- ikke utdatert
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

            val query =
                listOf(
                    selectKunRelevanteAapVedtak,
                    selectKunRelevante11_5Vedtak,
                ).joinToString("\nUNION ALL\n") + "ORDER BY aar DESC, lopenrsak DESC, lopenrvedtak DESC"

            connection.createParameterizedQuery(query).use { preparedStatement ->
                var p = 1 // parameter-indeks
                // S1: AAP-vedtak
                preparedStatement.setInt(p++, arenaPersonId)
                preparedStatement.setDate(p++, vedtakModnedGrense)
                preparedStatement.setDate(p++, vanligTidsbuffer)
                preparedStatement.setDate(p++, stansTidsbuffer)
                preparedStatement.setDate(p++, vanligTidsbuffer)
                // S2: 11-5-vedtak
                preparedStatement.setInt(p++, arenaPersonId)
                preparedStatement.setDate(p++, aa115BehandlingUkerGrense)

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
