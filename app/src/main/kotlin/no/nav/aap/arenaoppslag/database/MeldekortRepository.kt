package no.nav.aap.arenaoppslag.database

import no.nav.aap.arenaoppslag.modeller.Meldekort
import no.nav.aap.arenaoppslag.modeller.MeldekortDag
import no.nav.aap.arenaoppslag.modeller.MeldekortForSak
import no.nav.aap.arenaoppslag.modeller.MeldekortPostering
import no.nav.aap.arenaoppslag.modeller.MeldekortReduksjon
import no.nav.aap.arenaoppslag.modeller.Periode
import no.nav.aap.arenaoppslag.modeller.SakId
import org.intellij.lang.annotations.Language
import java.sql.Connection
import java.sql.ResultSet
import java.time.LocalDate
import javax.sql.DataSource

class MeldekortRepository(
    private val dataSource: DataSource,
    // Oracle har en hard grense på 1000 elementer i IN-lister. Vi chunker for å holde oss under denne grensen.
    private val chunkStørrelse: Int = 999,
) {

    fun hentForSak(sakId: SakId): MeldekortForSak = dataSource.connection.use { con ->
        MeldekortForSak(
            posteringer = selectPosteringer(sakId, con),
            meldekort = selectMeldekort(sakId, con),
        )
    }

    private fun selectPosteringer(sakId: SakId, connection: Connection): List<MeldekortPostering> =
        connection.createParameterizedQuery(posteringerForSakSql).use { preparedStatement ->
            preparedStatement.setInt(1, sakId.id)
            preparedStatement.executeQuery().map { row ->
                // wasNull() må sjekkes rett etter getLong, før vi leser andre kolonner.
                val meldekortId = row.getLong("meldekort_id").let { if (row.wasNull()) null else it }
                MeldekortPostering(
                    vedtakId = row.getInt("vedtak_id"),
                    personId = row.getInt("person_id"),
                    meldekortId = meldekortId,
                    periode = Periode(
                        fraOgMedDato = row.getDate("dato_periode_fra").toLocalDate(),
                        tilOgMedDato = row.getDate("dato_periode_til").toLocalDate(),
                    ),
                    belop = row.getInt("belop"),
                    dagsatsMedBarnetillegg = row.getString("dagsats_med_barnetillegg")?.toIntOrNull(),
                    dagsats = row.getString("dagsats")?.toIntOrNull(),
                    dagsatsForSamordning = row.getString("dagsats_for_samordning")?.toIntOrNull(),
                    insGrad = row.getString("ins_grad")?.toIntOrNull(),
                )
            }
        }

    private fun selectMeldekort(sakId: SakId, connection: Connection): List<Meldekort> {
        val metadata = connection.createParameterizedQuery(meldekortMetadataForSakSql).use { preparedStatement ->
            preparedStatement.setInt(1, sakId.id)
            preparedStatement.executeQuery().map { row -> mapMeldekortMetadata(row) }
        }
        if (metadata.isEmpty()) return emptyList()

        val meldekortIder = metadata.map { it.meldekortId }
        val dagerPerMeldekort = selectMeldekortdager(metadata.associateBy { it.meldekortId }, connection)
        val reduksjonPerMeldekort = selectAnmerkninger(meldekortIder, connection)

        return metadata.map { meta ->
            Meldekort(
                meldekortId = meta.meldekortId,
                personId = meta.personId,
                periode = Periode(meta.datoFra, meta.datoTil),
                ukenrUke1 = meta.ukenrUke1,
                ukenrUke2 = meta.ukenrUke2,
                meldedato = meta.meldedato,
                meldeform = meta.meldeform,
                fortsattRegistrertArbeidssoker = meta.fortsattArbeidssoker,
                kommentar = meta.kommentar,
                dager = dagerPerMeldekort[meta.meldekortId].orEmpty(),
                reduksjon = reduksjonPerMeldekort[meta.meldekortId] ?: MeldekortReduksjon(0, 0.0f, 0.0f),
            )
        }
    }

    private fun selectMeldekortdager(
        metadataPerId: Map<Long, MeldekortMetadata>,
        connection: Connection,
    ): Map<Long, List<MeldekortDag>> {
        if (metadataPerId.isEmpty()) return emptyMap()
        return metadataPerId.keys.chunked(chunkStørrelse).flatMap { chunk ->
            val sql = meldekortdagerSql(chunk)
            connection.createParameterizedQuery(sql).use { preparedStatement ->
                preparedStatement.executeQuery().map { row ->
                    val meldekortId = row.getLong("meldekort_id")
                    val ukenr = row.getInt("ukenr")
                    val dagnr = row.getInt("dagnr")
                    val meta = metadataPerId.getValue(meldekortId)
                    meldekortId to MeldekortDag(
                        ukenr = ukenr,
                        dagnr = dagnr,
                        // Meldekortdag har ikke dato — den utledes fra periodens startdato (mandag i uke 1).
                        dato = meta.datoFra.plusDays(((ukenr - meta.ukenrUke1) * DAGER_PER_UKE + (dagnr - 1)).toLong()),
                        timerArbeidet = row.getDouble("timer_arbeidet"),
                        annetFravaer = row.getString("status_annetfravaer") == "J",
                    )
                }
            }
        }.groupBy({ it.first }, { it.second })
    }

    private fun selectAnmerkninger(
        meldekortIder: List<Long>,
        connection: Connection,
    ): Map<Long, MeldekortReduksjon> {
        if (meldekortIder.isEmpty()) return emptyMap()
        return meldekortIder.chunked(chunkStørrelse).flatMap { chunk ->
            val sql = anmerkningerForMeldekortlisteSql(chunk)
            connection.createParameterizedQuery(sql).use { preparedStatement ->
                preparedStatement.executeQuery().map { row ->
                    row.getLong("objekt_id") to MeldekortReduksjon(
                        dagerForSent = row.getFloat("for_sent").toInt(),
                        fravar = row.getFloat("fravar"),
                        sykedager = row.getFloat("sykedager"),
                    )
                }
            }
        }.toMap()
    }

    private fun mapMeldekortMetadata(row: ResultSet) = MeldekortMetadata(
        meldekortId = row.getLong("meldekort_id"),
        personId = row.getInt("person_id"),
        datoFra = row.getDate("dato_fra").toLocalDate(),
        datoTil = row.getDate("dato_til").toLocalDate(),
        ukenrUke1 = row.getInt("ukenr_uke1"),
        ukenrUke2 = row.getInt("ukenr_uke2"),
        meldedato = row.getDate("dato_innkommet")?.toLocalDate(),
        meldeform = row.getString("mkskortkode"),
        fortsattArbeidssoker = tilBoolean(row.getString("status_fortsatt_arbeidsoker")),
        kommentar = row.getString("kommentar"),
    )

    private fun tilBoolean(verdi: String?): Boolean? = when (verdi) {
        "J" -> true
        "N" -> false
        else -> null
    }

    private data class MeldekortMetadata(
        val meldekortId: Long,
        val personId: Int,
        val datoFra: LocalDate,
        val datoTil: LocalDate,
        val ukenrUke1: Int,
        val ukenrUke2: Int,
        val meldedato: LocalDate?,
        val meldeform: String?,
        val fortsattArbeidssoker: Boolean?,
        val kommentar: String?,
    )

    @Language("OracleSql")
    private val posteringerForSakSql = """
        SELECT p.vedtak_id, p.person_id, p.meldekort_id, p.dato_periode_fra, p.dato_periode_til, p.belop,
               (SELECT MAX(vf.vedtakverdi)
                  FROM vedtakfakta vf
                 WHERE vf.vedtak_id = p.vedtak_id
                   AND vf.vedtakfaktakode = 'DAGSMBT') AS dagsats_med_barnetillegg,
               (SELECT MAX(vf.vedtakverdi)
                  FROM vedtakfakta vf
                 WHERE vf.vedtak_id = p.vedtak_id
                   AND vf.vedtakfaktakode = 'DAGS') AS dagsats,
               (SELECT MAX(vf.vedtakverdi)
                  FROM vedtakfakta vf
                 WHERE vf.vedtak_id = p.vedtak_id
                   AND vf.vedtakfaktakode = 'DAGSFSAM') AS dagsats_for_samordning,
               (SELECT MAX(vf.vedtakverdi)
                  FROM vedtakfakta vf
                 WHERE vf.vedtak_id = p.vedtak_id
                   AND vf.vedtakfaktakode = 'INSGRAD') AS ins_grad
          FROM postering p
          JOIN vedtak v ON v.vedtak_id = p.vedtak_id
         WHERE v.sak_id = ?
         ORDER BY p.dato_periode_fra, p.postering_id
    """.trimIndent()

    @Language("OracleSql")
    private val meldekortMetadataForSakSql = """
        SELECT DISTINCT m.meldekort_id, m.person_id, m.dato_innkommet, m.mkskortkode,
                        m.status_fortsatt_arbeidsoker, m.kommentar,
                        mkp.dato_fra, mkp.dato_til, mkp.ukenr_uke1, mkp.ukenr_uke2
          FROM meldekort m
          JOIN postering p ON p.meldekort_id = m.meldekort_id
          JOIN vedtak v ON v.vedtak_id = p.vedtak_id
          JOIN meldekortperiode mkp ON mkp.aar = m.aar AND mkp.periodekode = m.periodekode
         WHERE v.sak_id = ?
         ORDER BY mkp.dato_fra, m.meldekort_id
    """.trimIndent()

    // Oracle støtter ikke listeparametere i PreparedStatement, så meldekort-IDer interpoleres direkte.
    private fun meldekortdagerSql(meldekortIder: List<Long>): String {
        val idListe = meldekortIder.joinToString(",")
        return """
            SELECT meldekort_id, ukenr, dagnr, timer_arbeidet, status_annetfravaer
              FROM meldekortdag
             WHERE meldekort_id IN ($idListe)
             ORDER BY meldekort_id, ukenr, dagnr
        """.trimIndent()
    }

    // Oracle støtter ikke listeparametere i PreparedStatement, så meldekort-IDer interpoleres direkte.
    private fun anmerkningerForMeldekortlisteSql(meldekortIder: List<Long>): String {
        val idListe = meldekortIder.joinToString(",")
        return """
            SELECT objekt_id,
                   sum(CASE WHEN anmerkningkode = 'SENN' THEN verdi ELSE 0 END) AS for_sent,
                   sum(CASE WHEN anmerkningkode = 'FXNN' THEN verdi ELSE 0 END) AS fravar,
                   sum(CASE WHEN anmerkningkode = 'FSNN' THEN verdi ELSE 0 END) AS sykedager
              FROM anmerkning
             WHERE tabellnavnalias = 'MKORT'
               AND objekt_id IN ($idListe)
               AND anmerkningkode IN ('SENN', 'FXNN', 'FSNN')
             GROUP BY objekt_id
        """.trimIndent()
    }

    private companion object {
        private const val DAGER_PER_UKE = 7
    }
}



